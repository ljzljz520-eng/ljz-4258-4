package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.dto.Requests.*;
import com.dairy.homogenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferenceAdminService {
    private final ProductBatchRepository batches;
    private final SamplePointRepository points;
    private final StabilityWindowRepository windows;
    private final InstrumentAlgorithmVersionRepository algorithms;

    public ReferenceAdminService(ProductBatchRepository batches, SamplePointRepository points,
                                 StabilityWindowRepository windows,
                                 InstrumentAlgorithmVersionRepository algorithms) {
        this.batches = batches; this.points = points; this.windows = windows; this.algorithms = algorithms;
    }

    @Transactional
    public ProductBatch createBatch(CreateBatchRequest request) {
        batches.findByBatchNumber(request.batchNumber()).ifPresent(b -> { throw new IllegalArgumentException("批已存在"); });
        ProductBatch b = new ProductBatch();
        b.setBatchNumber(request.batchNumber()); b.setProductCode(request.productCode());
        b.setProductName(request.productName()); b.setLayerName(request.layerName());
        b.setStartedAt(request.startedAt()); b.setEndedAt(request.endedAt());
        return batches.save(b);
    }

    @Transactional
    public SamplePoint createPoint(CreateSamplePointRequest request) {
        points.findByCode(request.code()).ifPresent(p -> { throw new IllegalArgumentException("取样点已存在"); });
        SamplePoint p = new SamplePoint();
        p.setCode(request.code()); p.setName(request.name()); p.setValveGroupId(request.valveGroupId());
        p.setSamplingLine(request.samplingLine());
        p.setProductFlowOrder(request.productFlowOrder() == null ? 0 : request.productFlowOrder());
        return points.save(p);
    }

    @Transactional
    public StabilityWindow createWindow(CreateStableWindowRequest request) {
        if (!request.endedAt().isAfter(request.startedAt())) throw new IllegalArgumentException("稳定窗口结束必须晚于开始");
        StabilityWindow w = new StabilityWindow();
        w.setBatchId(request.batchId()); w.setValveGroupId(request.valveGroupId());
        w.setStageCode(request.stageCode()); w.setStartedAt(request.startedAt()); w.setEndedAt(request.endedAt());
        w.setNominalPressureBar(request.nominalPressureBar()); w.setNotes(request.notes());
        return windows.save(w);
    }

    @Transactional
    public InstrumentAlgorithmVersion createAlgorithm(AlgorithmVersionRequest request) {
        algorithms.findByInstrumentCodeAndMeasurementKindAndAlgorithmVersion(
                request.instrumentCode(), request.measurementKind(), request.algorithmVersion())
                .ifPresent(a -> { throw new IllegalArgumentException("算法版本已存在"); });
        InstrumentAlgorithmVersion a = new InstrumentAlgorithmVersion();
        a.setInstrumentCode(request.instrumentCode()); a.setMeasurementKind(request.measurementKind());
        a.setAlgorithmVersion(request.algorithmVersion()); a.setEffectiveFrom(request.effectiveFrom());
        a.setEffectiveTo(request.effectiveTo()); a.setDescription(request.description());
        a.setStatus(AlgorithmStatus.ACTIVE);
        algorithms.findByInstrumentCodeAndMeasurementKindAndStatus(
                request.instrumentCode(), request.measurementKind(), AlgorithmStatus.ACTIVE).ifPresent(previous -> {
            previous.setStatus(AlgorithmStatus.INACTIVE);
            previous.setEffectiveTo(request.effectiveFrom());
            algorithms.save(previous);
        });
        return algorithms.save(a);
    }
}
