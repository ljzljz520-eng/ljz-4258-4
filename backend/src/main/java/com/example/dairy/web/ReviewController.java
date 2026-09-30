package com.example.dairy.web;

import com.example.dairy.dto.Dtos.ReviewDecisionRequest;
import com.example.dairy.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/segments/{segmentId}/review")
public class ReviewController {
    private final EvidenceService evidence;
    public ReviewController(EvidenceService evidence) { this.evidence = evidence; }

    @PostMapping("/lock")
    @PreAuthorize("hasRole('REVIEWER')")
    public void lock(@PathVariable UUID segmentId, Authentication auth) { evidence.lockEvidence(segmentId, auth.getName()); }

    @PostMapping("/decision")
    @PreAuthorize("hasRole('REVIEWER')")
    public void decide(@PathVariable UUID segmentId, @Valid @RequestBody ReviewDecisionRequest request, Authentication auth) {
        evidence.decide(segmentId, request, auth.getName());
    }

    @PostMapping("/release")
    @PreAuthorize("hasRole('REVIEWER')")
    public void release(@PathVariable UUID segmentId, Authentication auth) { evidence.release(segmentId, auth.getName()); }
}
