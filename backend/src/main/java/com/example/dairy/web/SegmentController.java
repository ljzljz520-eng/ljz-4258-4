package com.example.dairy.web;

import com.example.dairy.dto.Dtos.*;
import com.example.dairy.service.DtoAssembler;
import com.example.dairy.service.PairingService;
import com.example.dairy.service.SegmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/segments")
public class SegmentController {
    private final SegmentService segments;
    private final PairingService pairing;
    private final DtoAssembler assembler;

    public SegmentController(SegmentService segments, PairingService pairing, DtoAssembler assembler) {
        this.segments = segments; this.pairing = pairing; this.assembler = assembler;
    }

    @GetMapping
    public List<SegmentResponse> list() { return segments.list(); }

    @GetMapping("/{id}")
    public SegmentDetailResponse detail(@PathVariable UUID id) { return segments.detail(id); }

    @PostMapping("/{id}/auto-pair")
    @PreAuthorize("hasAnyRole('OPERATOR','LAB_TECH')")
    public IdResponse autoPair(@PathVariable UUID id, Authentication auth) {
        pairing.autoPair(id, auth.getName());
        return new IdResponse(id);
    }

    @PostMapping("/{id}/comparisons")
    @PreAuthorize("hasRole('LAB_TECH')")
    public ComparisonResponse manualPair(@PathVariable UUID id, @RequestBody ManualPairRequest request, Authentication auth) {
        return assembler.comparison(pairing.manualPair(id, request, auth.getName()));
    }
}
