package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "evidence_snapshots")
public class EvidenceSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "review_id", nullable = false, unique = true) private Long reviewId;
    @Column(nullable = false) private String fingerprint;
    @Lob @Column(name = "snapshot_json", nullable = false, columnDefinition = "text") private String snapshotJson;
    @Column(name = "created_by", nullable = false) private String createdBy;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getReviewId() { return reviewId; } public void setReviewId(Long v) { this.reviewId = v; }
    public String getFingerprint() { return fingerprint; } public void setFingerprint(String v) { this.fingerprint = v; }
    public String getSnapshotJson() { return snapshotJson; } public void setSnapshotJson(String v) { this.snapshotJson = v; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String v) { this.createdBy = v; }
}
