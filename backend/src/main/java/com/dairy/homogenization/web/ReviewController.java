package com.dairy.homogenization.web;

import com.dairy.homogenization.dto.Requests.ReviewDecisionRequest;
import com.dairy.homogenization.repository.ReviewRepository;
import com.dairy.homogenization.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviews;
    private final ReviewRepository repository;
    private final ViewAssembler views;

    public ReviewController(ReviewService reviews, ReviewRepository repository, ViewAssembler views) {
        this.reviews = reviews; this.repository = repository; this.views = views;
    }

    @PostMapping("/comparisons/{id}/request")
    @PreAuthorize("hasAnyRole('LAB_ANALYST','REVIEWER')")
    public Object request(@PathVariable Long id, Principal principal) {
        return views.review(reviews.requestReview(id, principal.getName()));
    }

    @PostMapping("/comparisons/{id}/decision")
    @PreAuthorize("hasRole('REVIEWER')")
    public Object decision(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request,
                           Principal principal) {
        return views.review(reviews.decide(id, request.approve(), request.decisionNote(), principal.getName()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Object list() { return repository.findAll().stream().map(views::review).toList(); }
}
