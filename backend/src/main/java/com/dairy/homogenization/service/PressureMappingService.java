package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.dto.Requests.*;
import com.dairy.homogenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PressureMappingService {
    private final ValveGroupRepository valveGroups;
    private final PressureMappingVersionRepository versions;
    private final PressureObservationRepository observations;
    private final ComparisonMaintenanceService maintenance;
    private final ImportJobRepository importJobs;
    private final ProductBatchRepository productBatches;

    public PressureMappingService(ValveGroupRepository valveGroups, PressureMappingVersionRepository versions,
                                  PressureObservationRepository observations,
                                  ComparisonMaintenanceService maintenance,
                                  ImportJobRepository importJobs, ProductBatchRepository productBatches) {
        this.valveGroups = valveGroups; this.versions = versions; this.observations = observations;
        this.maintenance = maintenance; this.importJobs = importJobs; this.productBatches = productBatches;
    }

    @Transactional
    public PressureMappingVersion correct(CorrectMappingRequest request, String user) {
        if (!importJobs.findActive().isEmpty())
            throw new IllegalStateException("存在接收/处理中的仪器导入，不能修正校准映射；请先完成或取消并重试。");
        // Valve-group row lock makes correction vs. concurrent review/import deterministic.
        ValveGroup vg = valveGroups.findByIdForUpdate(request.valveGroupId())
                .orElseThrow(() -> new IllegalArgumentException("阀组不存在"));
        for (ProductBatch batch : productBatches.findAll()) {
            productBatches.findByIdForUpdate(batch.getId());
        }
        PressureMappingVersion old = versions.findByValveGroupIdAndStatus(vg.getId(), MappingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("阀组尚无 ACTIVE 压力映射"));
        int next = versions.findMaxVersion(vg.getId()).orElse(0) + 1;
        OffsetDateTime now = OffsetDateTime.now();

        old.setStatus(MappingStatus.SUPERSEDED);
        old.setEffectiveTo(now);
        PressureMappingVersion replacement = new PressureMappingVersion();
        replacement.setValveGroupId(vg.getId());
        replacement.setVersionNumber(next);
        replacement.setStatus(MappingStatus.ACTIVE);
        replacement.setEffectiveFrom(now);
        replacement.setCorrectedBy(user);
        replacement.setCorrectedAt(now);
        replacement.setChangeSummary(request.changeSummary());
        int order = 0;
        for (MappingItemInput item : request.items()) {
            PressureMappingItem mapped = new PressureMappingItem();
            mapped.setPressureMappingVersion(replacement);
            mapped.setRawLabel(item.rawLabel());
            mapped.setCalibratedStageCode(item.calibratedStageCode());
            mapped.setNominalPressureBar(item.nominalPressureBar());
            mapped.setDisplayOrder(item.displayOrder() == null ? order : item.displayOrder());
            replacement.getItems().add(mapped);
            order++;
        }
        PressureMappingVersion saved = versions.save(replacement);

        // Raw labels remain immutable on observations; only calibrated mapping is re-resolved.
        Map<String, String> labelToStage = new HashMap<>();
        for (PressureMappingItem i : saved.getItems()) labelToStage.put(i.getRawLabel(), i.getCalibratedStageCode());
        maintenance.expireComparisonsForMappingCorrection(vg.getId(), saved.getId());
        maintenance.refreshObservationsForValveGroup(vg.getId(), saved, labelToStage);
        maintenance.rebuildValveGroup(vg.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PressureMappingVersion> history(Long valveGroupId) {
        return versions.findByValveGroupIdOrderByVersionNumberDesc(valveGroupId);
    }
}
