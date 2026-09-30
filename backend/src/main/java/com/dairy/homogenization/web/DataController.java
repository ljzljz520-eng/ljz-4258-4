package com.dairy.homogenization.web;

import com.dairy.homogenization.dto.Views.PressurePointView;
import com.dairy.homogenization.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DataController {
    private final SampleRepository samples;
    private final ProductBatchRepository batches;
    private final PressureObservationRepository pressure;
    private final StabilityWindowRepository windows;
    private final ViewAssembler views;

    public DataController(SampleRepository samples, ProductBatchRepository batches,
                          PressureObservationRepository pressure, StabilityWindowRepository windows,
                          ViewAssembler views) {
        this.samples = samples; this.batches = batches; this.pressure = pressure; this.windows = windows;
        this.views = views;
    }

    @GetMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public Object batches() { return batches.findAll(); }

    @GetMapping("/batches/{id}/samples")
    @PreAuthorize("isAuthenticated()")
    public Object samples(@PathVariable Long id) {
        return samples.findByBatchIdOrderBySampledAtAsc(id).stream().map(views::sample).toList();
    }

    @GetMapping("/batches/{id}/pressure")
    @PreAuthorize("isAuthenticated()")
    public Object pressure(@PathVariable Long id, @RequestParam(required = false) Long valveGroupId) {
        if (valveGroupId == null) return pressure.findAll().stream().filter(p -> p.getBatchId().equals(id)).map(this::point).toList();
        return pressure.findByBatchIdAndValveGroupIdOrderByObservedAtAsc(id, valveGroupId).stream().map(this::point).toList();
    }

    @GetMapping("/batches/{id}/stable-windows")
    @PreAuthorize("isAuthenticated()")
    public Object windows(@PathVariable Long id) {
        return windows.findAll().stream().filter(w -> w.getBatchId().equals(id)).toList();
    }

    @GetMapping("/samples/{id}/curve")
    @PreAuthorize("isAuthenticated()")
    public Object curve(@PathVariable Long id) { return views.curve(id); }

    private PressurePointView point(com.dairy.homogenization.domain.PressureObservation p) {
        return new PressurePointView(p.getObservedAt(), p.getRawLabel(), p.getCalibratedStageCode(),
                p.getPressureMappingVersionId(), p.getObservedPressureBar());
    }
}
