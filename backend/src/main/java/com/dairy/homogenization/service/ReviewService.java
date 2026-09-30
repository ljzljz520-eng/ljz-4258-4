package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final EvidenceSnapshotRepository snapshots;
    private final ComparisonCandidateRepository comparisons;
    private final ValveGroupRepository valveGroups;
    private final ProductBatchRepository productBatches;
    private final ImportJobRepository jobs;
    private final ComparisonRuleApplicationService rules;
    private final ObjectMapper objectMapper;

    public ReviewService(ReviewRepository reviews, EvidenceSnapshotRepository snapshots,
                         ComparisonCandidateRepository comparisons, ValveGroupRepository valveGroups,
                         ImportJobRepository jobs, ComparisonRuleApplicationService rules,
                         ProductBatchRepository productBatches, ObjectMapper objectMapper) {
        this.reviews = reviews; this.snapshots = snapshots; this.comparisons = comparisons;
        this.valveGroups = valveGroups; this.jobs = jobs; this.rules = rules;
        this.productBatches = productBatches; this.objectMapper = objectMapper;
    }

    @Transactional
    public Review requestReview(Long comparisonId, String user) {
        ComparisonCandidate c = comparisons.findByIdForUpdate(comparisonId)
                .orElseThrow(() -> new IllegalArgumentException("比较不存在"));
        if (c.getStatus() == ComparisonStatus.APPROVED) throw new IllegalStateException("比较已审核并锁定");
        c = rules.evaluateCandidate(c);
        if (!c.isComparable()) throw new IllegalStateException("不可比比较不能送审：" + c.getReasonSummary());
        if (!jobs.findActive().isEmpty())
            throw new IllegalStateException("仍有导入任务在接收/处理中，禁止在导入途中签发或送审");
        Review existing = reviews.findByComparisonId(comparisonId).orElse(null);
        if (existing != null && existing.getStatus() == ReviewStatus.PENDING) return existing;
        Review r = existing == null ? new Review() : existing;
        r.setComparisonId(comparisonId);
        r.setStatus(ReviewStatus.PENDING);
        r.setRequestedBy(user);
        r.setRequestedAt(OffsetDateTime.now());
        r.setReviewedBy(null); r.setReviewedAt(null); r.setDecisionNote(null);
        return reviews.save(r);
    }

    @Transactional
    public Review decide(Long comparisonId, boolean approve, String note, String reviewer) {
        // Locking valve group, comparison and review serializes review against pressure-level correction.
        ComparisonCandidate c = comparisons.findByIdForUpdate(comparisonId)
                .orElseThrow(() -> new IllegalArgumentException("比较不存在"));
        valveGroups.findByIdForUpdate(c.getValveGroupId());
        // Batch lock prevents an import transaction changing evidence for this batch concurrently.
        productBatches.findByIdForUpdate(c.getBatchId());
        if (!jobs.findActive().isEmpty()) throw new IllegalStateException("导入仍在进行，不能签发");
        c = rules.evaluateCandidate(c);
        Review r = reviews.findByComparisonIdForUpdate(comparisonId)
                .orElseThrow(() -> new IllegalStateException("该比较尚未送审"));
        if (r.getStatus() != ReviewStatus.PENDING) throw new IllegalStateException("该审核已有终态，不能重复决策");
        String currentFingerprint = c.getFingerprint();
        if (approve) {
            if (!c.isComparable()) throw new IllegalStateException("强制可比检查未通过，不能批准：" + c.getReasonSummary());
            EvidenceSnapshot snapshot = snapshots.findAll().stream()
                    .filter(s -> Objects.equals(s.getReviewId(), r.getId())).findFirst().orElseGet(EvidenceSnapshot::new);
            Map<String, Object> payload = snapshotPayload(c, currentFingerprint, reviewer);
            snapshot.setReviewId(r.getId());
            snapshot.setFingerprint(currentFingerprint);
            snapshot.setSnapshotJson(toJson(payload));
            snapshot.setCreatedBy(reviewer);
            snapshots.save(snapshot);
            r.setStatus(ReviewStatus.APPROVED);
            c.setStatus(ComparisonStatus.APPROVED);
        } else {
            r.setStatus(ReviewStatus.RETURNED);
            c.setStatus(ComparisonStatus.CONFIRMED);
        }
        r.setReviewedBy(reviewer);
        r.setReviewedAt(OffsetDateTime.now());
        r.setDecisionNote(note);
        reviews.save(r); comparisons.save(c);
        return r;
    }

    private Map<String, Object> snapshotPayload(ComparisonCandidate c, String fingerprint, String reviewer) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "DAIRY_HOMOGENIZATION_EVIDENCE_SNAPSHOT");
        map.put("comparisonId", c.getId());
        map.put("fingerprint", fingerprint);
        map.put("reviewer", reviewer);
        map.put("lockedAt", OffsetDateTime.now().toString());
        map.put("beforeSampleId", c.getBeforeSampleId());
        map.put("afterSampleId", c.getAfterSampleId());
        map.put("stageCode", c.getStageCode());
        map.put("beforePressureMappingVersionId", c.getBeforePressureMappingVersionId());
        map.put("afterPressureMappingVersionId", c.getAfterPressureMappingVersionId());
        map.put("beforeAlgorithmVersionId", c.getBeforeAlgorithmVersionId());
        map.put("afterAlgorithmVersionId", c.getAfterAlgorithmVersionId());
        map.put("beforeMeasurementId", c.getBeforeMeasurementId());
        map.put("afterMeasurementId", c.getAfterMeasurementId());
        map.put("comparable", c.isComparable());
        map.put("note", "系统仅复核证据，不推荐压力，也不能控制均质机。");
        return map;
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
