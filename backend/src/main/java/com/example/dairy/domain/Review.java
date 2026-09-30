package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="reviews")
public class Review {
 @Id private UUID id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="segment_id") private BatchSegment segment;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Enums.ReviewStatus status;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="evidence_snapshot_id") private EvidenceSnapshot evidenceSnapshot;
 @Column(name="locked_by",nullable=false) private String lockedBy;
 @Column(name="locked_at",nullable=false) private Instant lockedAt=Instant.now();
 @Column(name="decision_by") private String decisionBy;
 @Column(name="decision_at") private Instant decisionAt;
 @Enumerated(EnumType.STRING) @Column(length=10) private Enums.ReviewDecision decision;
 private String notes;
 @Version private long lockVersion;
 @PrePersist void assign(){if(id==null)id=UUID.randomUUID();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public BatchSegment getSegment(){return segment;} public void setSegment(BatchSegment v){segment=v;}
 public Enums.ReviewStatus getStatus(){return status;} public void setStatus(Enums.ReviewStatus v){status=v;}
 public EvidenceSnapshot getEvidenceSnapshot(){return evidenceSnapshot;} public void setEvidenceSnapshot(EvidenceSnapshot v){evidenceSnapshot=v;}
 public String getLockedBy(){return lockedBy;} public void setLockedBy(String v){lockedBy=v;}
 public Instant getLockedAt(){return lockedAt;} public void setLockedAt(Instant v){lockedAt=v;}
 public String getDecisionBy(){return decisionBy;} public void setDecisionBy(String v){decisionBy=v;}
 public Instant getDecisionAt(){return decisionAt;} public void setDecisionAt(Instant v){decisionAt=v;}
 public Enums.ReviewDecision getDecision(){return decision;} public void setDecision(Enums.ReviewDecision v){decision=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
 public long getLockVersion(){return lockVersion;} public void setLockVersion(long v){lockVersion=v;}
}
