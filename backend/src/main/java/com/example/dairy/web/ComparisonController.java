package com.example.dairy.web;

import com.example.dairy.dto.Dtos.ComparisonResponse;
import com.example.dairy.service.DtoAssembler;
import com.example.dairy.service.PairingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/comparisons/{id}")
public class ComparisonController {
    private final PairingService pairing;
    private final DtoAssembler assembler;
    public ComparisonController(PairingService pairing, DtoAssembler assembler) { this.pairing=pairing; this.assembler=assembler; }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('LAB_TECH')")
    public ComparisonResponse refresh(@PathVariable UUID id) { return assembler.comparison(pairing.refresh(id)); }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('LAB_TECH')")
    public ComparisonResponse confirm(@PathVariable UUID id, Authentication auth) {
        return assembler.comparison(pairing.confirm(id, auth.getName()));
    }
}
