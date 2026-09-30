package com.dairy.homogenization.service;

import com.dairy.homogenization.config.AppProperties;
import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service
public class ComparisonRuleApplicationService {
    private final SampleRepository samples;
    private final SamplePointRepository points;
    private final ProductBatchRepository batches;
    private final StabilityWindowRepository windows;
    private final PressureObservationRepository pressureObservations;
    private final ParticleMeasurementRepository measurements;
    private final InstrumentAlgorithmVersionRepository algorithms;
    private final PressureMappingVersionRepository mappings;
    private final ComparisonCandidateRepository comparisons;
    private final ComparabilityFindingRepository findings;
    private final ImportFileRepository importFiles;
    private final AppProperties properties;

    public ComparisonRuleApplicationService(SampleRepository samples, SamplePointRepository points,
                                            ProductBatchRepository batches, StabilityWindowRepository windows,
                                            PressureObservationRepository pressureObservations,
                                            ParticleMeasurementRepository measurements,
                                            InstrumentAlgorithmVersionRepository algorithms,
                                            PressureMappingVersionRepository mappings,
                                            ComparisonCandidateRepository comparisons,
                                            ComparabilityFindingRepository findings,
                                            ImportFileRepository importFiles, AppProperties properties) {
        this.samples = samples; this.points = points; this.batches = batches; this.windows = windows;
        this.pressureObservations = pressureObservations; this.measurements = measurements;
        this.algorithms = algorithms; this.mappings = mappings; this.comparisons = comparisons;
        this.findings = findings; this.importFiles = importFiles; this.properties = properties;
    }

    @Transactional
    public void evaluateBatch(Long batchId, boolean createPairs) {
        if (createPairs) autoPairBatch(batchId);
        for (ComparisonCandidate candidate : comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId)) {
            evaluateCandidate(candidate);
        }
    }

    @Transactional
    public ComparisonCandidate evaluateCandidate(ComparisonCandidate candidate) {
        if (candidate.getStatus() == ComparisonStatus.APPROVED) return candidate;
        findings.deleteByComparisonId(candidate.getId());
        findings.flush();
        Sample before = samples.findById(candidate.getBeforeSampleId()).orElseThrow();
        Sample after = samples.findById(candidate.getAfterSampleId()).orElseThrow();
        ParticleMeasurement beforeM = activeMeasurement(before.getId()).orElse(null);
        ParticleMeasurement afterM = activeMeasurement(after.getId()).orElse(null);

        PressureObservation beforeObs = nearestObservation(before, beforeM);
        PressureObservation afterObs = nearestObservation(after, afterM);
        String stage = afterObs != null ? afterObs.getCalibratedStageCode() : null;
        candidate.setStageCode(stage);
        candidate.setBeforeMeasurementId(beforeM == null ? null : beforeM.getId());
        candidate.setAfterMeasurementId(afterM == null ? null : afterM.getId());
        candidate.setBeforePressureMappingVersionId(beforeObs == null ? null : beforeObs.getPressureMappingVersionId());
        candidate.setAfterPressureMappingVersionId(afterObs == null ? null : afterObs.getPressureMappingVersionId());
        candidate.setBeforeAlgorithmVersionId(beforeM == null ? null : beforeM.getAlgorithmVersionId());
        candidate.setAfterAlgorithmVersionId(afterM == null ? null : afterM.getAlgorithmVersionId());

        ComparabilityEngine.Side beforeSide = side(before, beforeM, beforeObs);
        ComparabilityEngine.Side afterSide = side(after, afterM, afterObs);
        boolean inWindow = stage != null && inStableWindow(after, stage, afterObs);
        EvidenceStatus evidence = evidenceStatus(beforeM, afterM, beforeObs, afterObs);
        var result = ComparabilityEngine.evaluate(new ComparabilityEngine.Context(
                beforeSide, afterSide,
                Objects.equals(before.getBatchId(), after.getBatchId()),
                Objects.equals(before.getValveGroupId(), after.getValveGroupId()),
                Duration.ofMinutes(properties.getPairing().getMaxTransportDelayMinutes()),
                inWindow, stage, evidence.complete(), evidence.warnings()));

        for (ComparabilityEngine.Finding f : result.findings()) {
            ComparabilityFinding entity = new ComparabilityFinding();
            entity.setComparisonId(candidate.getId());
            entity.setCode(f.code()); entity.setSeverity(f.severity());
            entity.setMessage(f.message()); entity.setDetail(f.detail());
            findings.save(entity);
        }
        candidate.setComparable(result.comparable());
        candidate.setReasonSummary(result.summary());
        if (!result.comparable() && candidate.getStatus() == ComparisonStatus.CONFIRMED) {
            candidate.setStatus(ComparisonStatus.EXPIRED);
        }
        candidate.setFingerprint(fingerprint(candidate, result.findings()));
        return comparisons.save(candidate);
    }

    public void autoPairBatch(Long batchId) {
        List<Sample> all = samples.findByBatchIdOrderBySampledAtAsc(batchId);
        List<Sample> before = all.stream().filter(s -> s.getPosition() == SamplePosition.BEFORE).toList();
        List<Sample> after = all.stream().filter(s -> s.getPosition() == SamplePosition.AFTER).toList();
        for (Sample a : after) {
            for (Sample b : before) {
                if (!eligibleAutoPair(b, a)) continue;
                comparisons.findByBeforeSampleIdAndAfterSampleId(b.getId(), a.getId()).orElseGet(() -> {
                    ComparisonCandidate c = new ComparisonCandidate();
                    c.setBeforeSampleId(b.getId()); c.setAfterSampleId(a.getId());
                    c.setBatchId(batchId); c.setValveGroupId(a.getValveGroupId());
                    c.setOrigin(ComparisonOrigin.AUTO);
                    c.setStatus(ComparisonStatus.CANDIDATE);
                    return comparisons.save(c);
                });
            }
        }
    }

    private boolean eligibleAutoPair(Sample b, Sample a) {
        if (!Objects.equals(b.getValveGroupId(), a.getValveGroupId())) return false;
        if (!Objects.equals(b.getLayerName(), a.getLayerName())) return false;
        SamplePoint bp = points.findById(b.getSamplePointId()).orElseThrow();
        SamplePoint ap = points.findById(a.getSamplePointId()).orElseThrow();
        if (!Objects.equals(bp.getSamplingLine(), ap.getSamplingLine())) return false;
        Duration d = Duration.between(b.getSampledAt(), a.getSampledAt()).abs();
        // Chronological inversion is intentionally retained as a CANDIDATE so the UI can show
        // LATE_BEFORE_SAMPLE; auto pairing must not silently discard late pre-samples.
        return d.toMinutes() <= properties.getPairing().getMaxTransportDelayMinutes();
    }

    @Transactional
    public ComparisonCandidate createManualPair(Long beforeId, Long afterId, String stageCode, String user) {
        Sample b = samples.findById(beforeId).orElseThrow(() -> new IllegalArgumentException("前样不存在"));
        Sample a = samples.findById(afterId).orElseThrow(() -> new IllegalArgumentException("后样不存在"));
        if (b.getPosition() != SamplePosition.BEFORE || a.getPosition() != SamplePosition.AFTER)
            throw new IllegalArgumentException("必须选择一个前样和一个后样");
        ComparisonCandidate c = comparisons.findByBeforeSampleIdAndAfterSampleId(beforeId, afterId)
                .orElseGet(() -> {
                    ComparisonCandidate created = new ComparisonCandidate();
                    created.setBeforeSampleId(beforeId); created.setAfterSampleId(afterId);
                    created.setBatchId(a.getBatchId()); created.setValveGroupId(a.getValveGroupId());
                    return created;
                });
        c.setOrigin(ComparisonOrigin.MANUAL);
        c.setStatus(ComparisonStatus.CANDIDATE);
        c.setStageCode(stageCode);
        return evaluateCandidate(comparisons.save(c));
    }


    @Transactional
    public ComparisonCandidate confirm(Long comparisonId) {
        ComparisonCandidate c = comparisons.findByIdForUpdate(comparisonId)
                .orElseThrow(() -> new IllegalArgumentException("比较不存在"));
        c = evaluateCandidate(c);
        if (c.getStatus() == ComparisonStatus.APPROVED) throw new IllegalStateException("已审核证据锁定，不能改确认状态");
        if (!c.isComparable()) {
            throw new IllegalStateException("人工确认不能覆盖不可比阻断原因：" + c.getReasonSummary());
        }
        c.setStatus(ComparisonStatus.CONFIRMED);
        return comparisons.save(c);
    }

    @Transactional
    public ComparisonCandidate reject(Long comparisonId) {
        ComparisonCandidate c = comparisons.findByIdForUpdate(comparisonId)
                .orElseThrow(() -> new IllegalArgumentException("比较不存在"));
        if (c.getStatus() == ComparisonStatus.APPROVED) throw new IllegalStateException("已审核证据锁定，不能拒绝");
        c.setStatus(ComparisonStatus.REJECTED);
        return comparisons.save(c);
    }

    @Transactional(readOnly = true)
    public List<ComparisonCandidate> listBatch(Long batchId) {
        return comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId);
    }

    @Transactional(readOnly = true)
    public ComparisonCandidate get(Long id) {
        return comparisons.findById(id).orElseThrow(() -> new IllegalArgumentException("比较不存在"));
    }

    private ComparabilityEngine.Side side(Sample sample, ParticleMeasurement measurement, PressureObservation obs) {
        SamplePoint point = points.findById(sample.getSamplePointId()).orElseThrow();
        String algorithm = null;
        if (measurement != null) {
            algorithm = algorithms.findById(measurement.getAlgorithmVersionId()).map(InstrumentAlgorithmVersion::getAlgorithmVersion).orElse(null);
        }
        return new ComparabilityEngine.Side(sample.getSampleCode(), point.getCode(), point.getSamplingLine(),
                sample.getLayerName(), sample.getSampledAt(), obs == null ? null : obs.getCalibratedStageCode(),
                obs == null ? null : obs.getPressureMappingVersionId(), algorithm, measurement != null);
    }

    private Optional<ParticleMeasurement> activeMeasurement(Long sampleId) {
        return measurements.findBySampleIdAndStatus(sampleId, MeasurementStatus.ACTIVE);
    }

    private PressureObservation nearestObservation(Sample sample, ParticleMeasurement m) {
        OffsetDateTime anchor = sample.getSampledAt();
        return pressureObservations.findByBatchIdAndValveGroupIdOrderByObservedAtAsc(
                        sample.getBatchId(), sample.getValveGroupId()).stream()
                .min(Comparator.comparingLong(o -> Math.abs(Duration.between(o.getObservedAt(), anchor).toSeconds())))
                .orElse(null);
    }

    private boolean inStableWindow(Sample after, String stage, PressureObservation obs) {
        OffsetDateTime t = after.getSampledAt();
        long tolerance = properties.getPairing().getStableWindowToleranceSeconds();
        return windows.findByBatchIdAndValveGroupIdOrderByStartedAtAsc(after.getBatchId(), after.getValveGroupId()).stream()
                .filter(w -> Objects.equals(w.getStageCode(), stage))
                .anyMatch(w -> !t.isBefore(w.getStartedAt().minusSeconds(tolerance))
                        && !t.isAfter(w.getEndedAt().plusSeconds(tolerance)));
    }

    private EvidenceStatus evidenceStatus(ParticleMeasurement before, ParticleMeasurement after,
                                          PressureObservation beforeObs, PressureObservation afterObs) {
        List<Long> fileIds = new ArrayList<>();
        if (before != null && before.getImportFileId() != null) fileIds.add(before.getImportFileId());
        if (after != null && after.getImportFileId() != null) fileIds.add(after.getImportFileId());
        if (beforeObs != null && beforeObs.getImportFileId() != null) fileIds.add(beforeObs.getImportFileId());
        if (afterObs != null && afterObs.getImportFileId() != null) fileIds.add(afterObs.getImportFileId());
        List<String> warnings = new ArrayList<>();
        for (Long fileId : fileIds) {
            ImportFile file = importFiles.findById(fileId).orElse(null);
            if (file == null || file.getStatus() != ImportFileStatus.SUCCESS) return new EvidenceStatus(false, warnings);
            if (importFiles.findByJobIdAndStatusInOrderByIdAsc(file.getJobId(), List.of(
                    ImportFileStatus.PENDING, ImportFileStatus.PROCESSING, ImportFileStatus.FAILED, ImportFileStatus.RETRYING))
                    .stream().anyMatch(other -> !Objects.equals(other.getId(), fileId))) {
                warnings.add("导入任务 " + file.getJobId() + " 存在未成功文件，不能把单文件成功视为完整证据。");
                return new EvidenceStatus(false, warnings);
            }
        }
        return new EvidenceStatus(true, warnings);
    }

    private record EvidenceStatus(boolean complete, List<String> warnings) {}

    private String fingerprint(ComparisonCandidate c, List<ComparabilityEngine.Finding> fs) {
        String raw = String.join("|", String.valueOf(c.getBeforeSampleId()), String.valueOf(c.getAfterSampleId()),
                String.valueOf(c.getStageCode()), String.valueOf(c.getBeforePressureMappingVersionId()),
                String.valueOf(c.getAfterPressureMappingVersionId()), String.valueOf(c.getBeforeAlgorithmVersionId()),
                String.valueOf(c.getAfterAlgorithmVersionId()), String.valueOf(c.isComparable()),
                fs.stream().map(f -> f.code() + ":" + f.severity()).sorted().reduce("", (a,b)->a+b));
        try {
            return "%064x".formatted(new BigInteger(1, MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
