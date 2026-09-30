package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "reviews")
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "comparison_id", nullable = false, unique = true) private Long comparisonId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReviewStatus status = ReviewStatus.PENDING;
    @Column(name = "requested_by", nullable = false) private String requestedBy;
    @Column(name = "reviewed_by") private String reviewedBy;
    @Column(name = "requested_at", nullable = false) private OffsetDateTime requestedAt;
    @Column(name = "reviewed_at") private OffsetDateTime reviewedAt;
    @Column(name = "decision_note") private String decisionNote;
    @Version @Column(name = "lock_version") private Integer lockVersion;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getComparisonId() { return comparisonId; } public void setComparisonId(Long v) { this.comparisonId = v; }
    public ReviewStatus getStatus() { return status; } public void setStatus(ReviewStatus v) { this.status = v; }
    public String getRequestedBy() { return requestedBy; } public void setRequestedBy(String v) { this.requestedBy = v; }
    public String getReviewedBy() { return reviewedBy; } public void setReviewedBy(String v) { this.reviewedBy = v; }
    public OffsetDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(OffsetDateTime v) { this.requestedAt = v; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(OffsetDateTime v) { this.reviewedAt = v; }
    public String getDecisionNote() { return decisionNote; } public void setDecisionNote(String v) { this.decisionNote = v; }
    public Integer getLockVersion() { return lockVersion; } public void setLockVersion(Integer v) { this.lockVersion = v; }
}
