package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ComparisonMaintenanceService {
    private final ComparisonCandidateRepository comparisons;
    private final PressureObservationRepository observations;
    private final ComparisonRuleApplicationService rules;

    public ComparisonMaintenanceService(ComparisonCandidateRepository comparisons,
                                        PressureObservationRepository observations,
                                        ComparisonRuleApplicationService rules) {
        this.comparisons = comparisons;
        this.observations = observations;
        this.rules = rules;
    }

    @Transactional
    public void refreshObservationsForValveGroup(Long valveGroupId, PressureMappingVersion mapping,
                                                 Map<String, String> labelToStage) {
        // One valve group can span multiple batches; correction is level-wide.
        List<PressureObservation> all = observations.findAll().stream()
                .filter(o -> Objects.equals(o.getValveGroupId(), valveGroupId)).toList();
        for (PressureObservation o : all) {
            o.setCalibratedStageCode(labelToStage.get(o.getRawLabel()));
            o.setPressureMappingVersionId(mapping.getId());
        }
    }

    @Transactional
    public void expireComparisonsForMappingCorrection(Long valveGroupId, Long newMappingVersionId) {
        for (ComparisonCandidate c : comparisons.findByValveGroupIdAndStatusNot(valveGroupId, ComparisonStatus.APPROVED)) {
            if (c.getStatus() == ComparisonStatus.REJECTED) continue;
            c.setStatus(ComparisonStatus.EXPIRED);
            c.setComparable(false);
            c.setReasonSummary("压力校准级别映射已修正（新映射#" + newMappingVersionId
                    + "），依赖旧证据的比较自动过期，需重新自动配对/人工确认。");
        }
    }

    @Transactional
    public void rebuildValveGroup(Long valveGroupId) {
        Set<Long> batchIds = new TreeSet<>();
        comparisons.findByValveGroupIdAndStatusNot(valveGroupId, ComparisonStatus.APPROVED)
                .forEach(c -> batchIds.add(c.getBatchId()));
        observations.findAll().stream()
                .filter(o -> Objects.equals(o.getValveGroupId(), valveGroupId))
                .forEach(o -> batchIds.add(o.getBatchId()));
        for (Long batchId : batchIds) rules.evaluateBatch(batchId, false);
    }
}
