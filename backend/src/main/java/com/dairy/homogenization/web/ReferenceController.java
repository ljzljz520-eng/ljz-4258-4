package com.dairy.homogenization.web;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.dto.Requests.*;
import com.dairy.homogenization.repository.*;
import com.dairy.homogenization.service.PressureMappingService;
import com.dairy.homogenization.service.ReferenceAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api")
public class ReferenceController {
    private final ReferenceAdminService reference;
    private final PressureMappingService mappingService;
    private final PressureMappingVersionRepository mappingRepository;
    private final ValveGroupRepository valveGroups;
    private final ViewAssembler views;

    public ReferenceController(ReferenceAdminService reference, PressureMappingService mappingService,
                               PressureMappingVersionRepository mappingRepository, ValveGroupRepository valveGroups,
                               ViewAssembler views) {
        this.reference = reference; this.mappingService = mappingService;
        this.mappingRepository = mappingRepository; this.valveGroups = valveGroups; this.views = views;
    }

    @PostMapping("/batches") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATOR','LAB_ANALYST')")
    public ProductBatch batch(@Valid @RequestBody CreateBatchRequest request) { return reference.createBatch(request); }

    @PostMapping("/sample-points") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATOR','LAB_ANALYST')")
    public SamplePoint point(@Valid @RequestBody CreateSamplePointRequest request) { return reference.createPoint(request); }

    @PostMapping("/stable-windows") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LAB_ANALYST','REVIEWER')")
    public StabilityWindow window(@Valid @RequestBody CreateStableWindowRequest request) { return reference.createWindow(request); }

    @PostMapping("/algorithm-versions") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LAB_ANALYST')")
    public InstrumentAlgorithmVersion algorithm(@Valid @RequestBody AlgorithmVersionRequest request) {
        return reference.createAlgorithm(request);
    }

    @PostMapping("/pressure-mappings/correct") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LAB_ANALYST')")
    public Object correct(@Valid @RequestBody CorrectMappingRequest request, Principal principal) {
        return views.mapping(mappingService.correct(request, principal.getName()));
    }

    @GetMapping("/valve-groups/{id}/pressure-mappings")
    @PreAuthorize("isAuthenticated()")
    public Object mappingHistory(@PathVariable Long id) {
        valveGroups.findById(id).orElseThrow(() -> new IllegalArgumentException("阀组不存在"));
        return mappingService.history(id).stream().map(views::mapping).toList();
    }
}
