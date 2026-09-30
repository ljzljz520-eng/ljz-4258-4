package com.example.dairy.service;

import com.example.dairy.domain.*;
import com.example.dairy.dto.Dtos.ReviewDecisionRequest;
import com.example.dairy.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class EvidenceService {
    private final BatchSegmentRepository segments;
    private final SampleRepository samples;
    private final ComparisonRepository comparisons;
    private final ImportJobRepository jobs;
    private final ReviewRepository reviews;
    private final EvidenceSnapshotRepository snapshots;
    private final ObjectMapper mapper;

    public EvidenceService(BatchSegmentRepository segments, SampleRepository samples, ComparisonRepository comparisons,
                           ImportJobRepository jobs, ReviewRepository reviews, EvidenceSnapshotRepository snapshots) {
        this.segments = segments; this.samples = samples; this.comparisons = comparisons;
        this.jobs = jobs; this.reviews = reviews; this.snapshots = snapshots;
        this.mapper = new ObjectMapper().findAndRegisterModules()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Transactional
    public Review lockEvidence(UUID segmentId, String actor) {
        BatchSegment segment = segments.findById(segmentId).orElseThrow(() -> new NotFoundException("批段不存在"));
        if (segment.getStatus() == Enums.SegmentStatus.ISSUED) throw new ConflictException("批段已签发，不能重复锁定。");
        ensureEvidenceReady(segmentId);
        segment = segments.lockForUpdate(segmentId).orElseThrow();
        Review review = reviews.findBySegmentId(segmentId).orElseGet(() -> {
            Review r = new Review();
            r.setSegment(segments.lockForUpdate(segmentId).orElseThrow());
            r.setLockedBy(actor);
            r.setStatus(Enums.ReviewStatus.LOCKED);
            return reviews.save(r);
        });
        if (review.getStatus() == Enums.ReviewStatus.RELEASED) {
            review.setStatus(Enums.ReviewStatus.LOCKED); review.setLockedBy(actor); review.setLockedAt(Instant.now());
        }
        EvidenceSnapshot snapshot = createSnapshot(segment, actor);
        review.setEvidenceSnapshot(snapshot);
        segment.setStatus(Enums.SegmentStatus.LOCKED);
        return review;
    }

    @Transactional
    public Review decide(UUID segmentId, ReviewDecisionRequest request, String actor) {
        BatchSegment segment = segments.findById(segmentId).orElseThrow(() -> new NotFoundException("批段不存在"));
        Review review = reviews.findBySegmentId(segmentId).orElseThrow(() -> new ConflictException("请先锁定证据快照。"));
        if (review.getStatus() != Enums.ReviewStatus.LOCKED) throw new ConflictException("只有锁定状态可以签发。");
        ensureEvidenceReady(segmentId);
        segment = segments.lockForUpdate(segmentId).orElseThrow();
        String current = fingerprint(buildPayload(segment));
        if (!current.equals(review.getEvidenceSnapshot().getSha256()))
            throw new ConflictException("证据指纹已变化，锁定快照过期；请释放后重新锁定。");
        review.setDecision(request.decision());
        review.setDecisionBy(actor);
        review.setDecisionAt(Instant.now());
        review.setNotes(request.notes());
        review.setStatus(Enums.ReviewStatus.ISSUED);
        segment.setStatus(Enums.SegmentStatus.ISSUED);
        return review;
    }

    @Transactional
    public Review release(UUID segmentId, String actor) {
        BatchSegment segment = segments.findById(segmentId).orElseThrow(() -> new NotFoundException("批段不存在"));
        Review review = reviews.findBySegmentId(segmentId).orElseThrow(() -> new ConflictException("不存在锁定记录。"));
        if (review.getStatus() == Enums.ReviewStatus.ISSUED) throw new ConflictException("已签发审核不能释放。");
        segment = segments.lockForUpdate(segmentId).orElseThrow();
        review.setStatus(Enums.ReviewStatus.RELEASED);
        segment.setStatus(Enums.SegmentStatus.OPEN);
        return review;
    }

    private void ensureEvidenceReady(UUID segmentId) {
        List<ImportJob> all = jobs.findBySegmentIdOrderByCreatedAtAsc(segmentId);
        if (all.isEmpty() || all.get(all.size()-1).getStatus() != Enums.ImportStatus.SUCCEEDED)
            throw new ConflictException("最新导入任务未完整成功，防止导入途中签发。");
        List<Comparison> active = comparisons.findBySegmentIdAndStatusNotOrderByProposedAtDesc(segmentId, Enums.ComparisonStatus.SUPERSEDED);
        if (active.stream().noneMatch(c -> c.getStatus()==Enums.ComparisonStatus.CONFIRMED))
            throw new ConflictException("至少需要一个经实验员确认且当前可比的候选。");
        for (Comparison c : active) {
            if (c.getStatus() != Enums.ComparisonStatus.CONFIRMED) continue;
            if (!c.getReasons().isEmpty()) throw new ConflictException("已确认候选存在不可比原因: " + c.getReasons().get(0).getCode());
        }
        boolean everyAfterHasConfirmedCandidate = samples.findBySegmentIdAndSampleTypeOrderBySampledAtAsc(segmentId, Enums.SampleType.AFTER)
                .stream().allMatch(s -> active.stream().anyMatch(c -> c.getStatus()==Enums.ComparisonStatus.CONFIRMED
                        && c.getAfterSample().getId().equals(s.getId())));
        if (!everyAfterHasConfirmedCandidate) throw new ConflictException("每个后样压力阶段都必须有当前可比且已确认的候选。");
    }

    private EvidenceSnapshot createSnapshot(BatchSegment segment, String actor) {
        SortedMap<String,Object> payload = buildPayload(segment);
        String json = writeJson(payload);
        String hash = fingerprint(payload);
        EvidenceSnapshot snapshot = snapshots.findBySha256(hash).orElseGet(() -> {
            EvidenceSnapshot s = new EvidenceSnapshot(); s.setSegment(segment); s.setPayload(json); s.setSha256(hash); s.setCreatedBy(actor);
            return s;
        });
        if (snapshot.getPayload() == null) snapshot.setPayload(json);
        if (snapshot.getId() == null) snapshots.save(snapshot);
        return snapshot;
    }

    private SortedMap<String,Object> buildPayload(BatchSegment segment) {
        SortedMap<String,Object> root = new TreeMap<>();
        root.put("segmentCode", segment.getCode());
        root.put("samplingLine", segment.getSamplingLine().getCode());
        root.put("productBatch", segment.getProductBatch().getBatchNo());
        root.put("valveGroup", segment.getValveGroup().getCode());
        root.put("windows", orderedMap("baselineStart", segment.getBaselineStart(), "baselineEnd", segment.getBaselineEnd(),
                "stableStart", segment.getStableStart(), "stableEnd", segment.getStableEnd()));
        List<Map<String,Object>> samplePayload = new ArrayList<>();
        for (Sample s : samples.findBySegmentIdOrderBySampledAtAsc(segment.getId())) {
            SortedMap<String,Object> sm = new TreeMap<>();
            sm.put("sampleCode", s.getSampleCode()); sm.put("type", s.getSampleType()); sm.put("layer", s.getLayer());
            sm.put("sampledAt", s.getSampledAt()); sm.put("receivedAt", s.getReceivedAt());
            sm.put("algorithmVersion", s.getAlgorithmVersion().getInstrumentCode()+":"+s.getAlgorithmVersion().getAlgorithmName()+":"+s.getAlgorithmVersion().getVersion());
            sm.put("pressure", s.getPressureReadings().stream().sorted(Comparator.comparingInt(PressureReading::getStageOrder)).map(r -> orderedMap(
                    "order", r.getStageOrder(), "rawLabel", r.getRawLabel(), "rawBar", r.getRawValueBar(),
                    "correctedBar", r.getCorrectedValueBar(), "mappingId", r.getMapping()==null?null:r.getMapping().getId())).toList());
            sm.put("distribution", s.getParticleDistributions().stream().sorted(Comparator.comparing(ParticleDistribution::getBinUm)).map(d -> orderedMap(
                    "binUm", d.getBinUm(), "volumePct", d.getVolumePct())).toList());
            samplePayload.add(sm);
        }
        root.put("samples", samplePayload);
        root.put("comparisons", comparisons.findBySegmentIdAndStatusNotOrderByProposedAtDesc(segment.getId(), Enums.ComparisonStatus.SUPERSEDED).stream()
                .map(c -> orderedMap("id", c.getId(), "before", c.getBeforeSample().getSampleCode(), "after", c.getAfterSample().getSampleCode(),
                        "status", c.getStatus(), "mappingId", c.getPressureMapping()==null?null:c.getPressureMapping().getId(),
                        "reasons", c.getReasons().stream().map(r -> orderedMap("code",r.getCode(),"detail",r.getDetail())).toList())).toList());
        root.put("importJobs", jobs.findBySegmentIdOrderByCreatedAtAsc(segment.getId()).stream().map(j -> orderedMap(
                "id",j.getId(),"status",j.getStatus(),"total",j.getTotalFiles(),"success",j.getSuccessfulFiles(),"failed",j.getFailedFiles(),
                "files",j.getFiles().stream().map(f->orderedMap("filename",f.getFilename(),"checksum",f.getChecksum(),"status",f.getStatus())).toList())).toList());
        return root;
    }

    private String fingerprint(SortedMap<String,Object> payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(writeJson(payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha256:");
            for (byte b : hash) sb.append("%02x".formatted(b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String writeJson(Object value) {
        try { return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
    private SortedMap<String,Object> orderedMap(Object... kv) {
        SortedMap<String,Object> map = new TreeMap<>();
        for (int i=0;i<kv.length;i+=2) map.put(kv[i].toString(), kv[i+1]);
        return map;
    }
}
