package com.example.dairy.service;

import com.example.dairy.domain.*;
import com.example.dairy.dto.Dtos.CorrectionRequest;
import com.example.dairy.dto.Dtos.PressureMappingResponse;
import com.example.dairy.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PressureCorrectionService {
    private final BatchSegmentRepository segments;
    private final SampleRepository samples;
    private final PressureCalibrationMappingRepository mappings;
    private final ComparisonRepository comparisons;
    private final PressureMappingRevisionRepository revisions;
    private final AuditLogRepository audits;

    public PressureCorrectionService(BatchSegmentRepository segments, SampleRepository samples,
                                     PressureCalibrationMappingRepository mappings, ComparisonRepository comparisons,
                                     PressureMappingRevisionRepository revisions, AuditLogRepository audits) {
        this.segments = segments; this.samples = samples; this.mappings = mappings;
        this.comparisons = comparisons; this.revisions = revisions; this.audits = audits;
    }

    @Transactional
    public PressureMappingResponse correct(UUID segmentId, CorrectionRequest req, String actor) {
        BatchSegment segment = segments.lockForUpdate(segmentId).orElseThrow(() -> new NotFoundException("批段不存在"));
        if (segment.getStatus() != Enums.SegmentStatus.OPEN)
            throw new ConflictException("批段已锁定或签发；只能在锁证前进行压力修正。");

        PressureCalibrationMapping old = mappings.findFirstByValveGroup_IdAndMeasurementPoint_IdAndInstrumentLabelAndActiveOrderByVersionDesc(
                segment.getValveGroup().getId(), req.measurementPointId(), req.rawLabel(), true)
                .orElseThrow(() -> new NotFoundException("现行压力校准映射不存在"));
        if (req.correctionLevel() != old.getCorrectionLevel() + 1)
            throw new ValidationException("修正级别必须从 %d 递增到 %d。".formatted(old.getCorrectionLevel(), old.getCorrectionLevel()+1));

        old.setActive(false);
        PressureCalibrationMapping next = new PressureCalibrationMapping();
        next.setId(UUID.randomUUID());
        next.setValveGroup(old.getValveGroup());
        next.setMeasurementPoint(old.getMeasurementPoint());
        next.setInstrumentLabel(old.getInstrumentLabel());
        next.setCanonicalStageLabel(req.canonicalLabel());
        next.setCorrectionLevel(req.correctionLevel());
        next.setVersion(mappings.nextVersion(old.getValveGroup().getId(), old.getMeasurementPoint().getId(), old.getInstrumentLabel()));
        next.setSlope(req.slope());
        next.setIntercept(req.intercept());
        next.setEffectiveFrom(Instant.now());
        next.setActive(true);
        next.setSupersedesId(old.getId());
        next.setCreatedBy(actor);
        mappings.save(next);

        for (Sample sample : samples.findBySegmentIdOrderBySampledAtAsc(segmentId)) {
            for (PressureReading reading : sample.getPressureReadings()) {
                if (reading.getMapping() != null && reading.getMapping().getId().equals(old.getId())) {
                    reading.setMapping(next);
                    reading.setCorrectedValueBar(reading.getRawValueBar().multiply(req.slope()).add(req.intercept()));
                }
            }
        }

        PressureMappingRevision revision = new PressureMappingRevision();
        revision.setOldMappingId(old.getId());
        revision.setNewMapping(next);
        revision.setAffectedSegment(segment);
        revision.setCorrectionLevel(req.correctionLevel());
        revision.setReason(req.reason());
        revision.setCreatedBy(actor);
        revisions.save(revision);

        List<Comparison> dependent = comparisons.findActiveByDependencyMappingId(old.getId(), Enums.ComparisonStatus.SUPERSEDED);
        for (Comparison c : dependent) {
            c.setStatus(Enums.ComparisonStatus.STALE);
            c.setPressureMapping(next);
            boolean already = c.getReasons().stream().anyMatch(r -> r.getCode().equals("PRESSURE_MAPPING_CHANGED"));
            if (!already) {
                ComparisonReason reason = new ComparisonReason();
                reason.setComparison(c);
                reason.setCode("PRESSURE_MAPPING_CHANGED");
                reason.setDetail("压力修正级别由 %d 变为 %d；比较依赖已过期，必须刷新并重新人工确认。"
                        .formatted(old.getCorrectionLevel(), req.correctionLevel()));
                reason.setSeverity(Enums.Severity.BLOCKER);
                c.getReasons().add(reason);
            }
        }

        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction("PRESSURE_CORRECTION");
        log.setSegmentId(segmentId);
        log.setDetail("{\"oldMapping\":\"%s\",\"newMapping\":\"%s\",\"level\":%d}"
                .formatted(old.getId(), next.getId(), req.correctionLevel()));
        audits.save(log);

        return new PressureMappingResponse(next.getId(), next.getInstrumentLabel(), next.getCanonicalStageLabel(),
                next.getCorrectionLevel(), next.getVersion(), next.getSlope(), next.getIntercept(),
                next.getEffectiveFrom(), next.isActive());
    }
}
