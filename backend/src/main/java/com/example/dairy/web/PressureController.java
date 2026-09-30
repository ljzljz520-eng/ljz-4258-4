package com.example.dairy.web;

import com.example.dairy.dto.Dtos.CorrectionRequest;
import com.example.dairy.dto.Dtos.PressureMappingResponse;
import com.example.dairy.service.PressureCorrectionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/segments/{segmentId}/pressure-mappings")
public class PressureController {
    private final PressureCorrectionService service;
    public PressureController(PressureCorrectionService service) { this.service = service; }

    @PostMapping("/corrections")
    @PreAuthorize("hasRole('LAB_TECH')")
    public PressureMappingResponse correct(@PathVariable UUID segmentId, @Valid @RequestBody CorrectionRequest req, Authentication auth) {
        return service.correct(segmentId, req, auth.getName());
    }
}
