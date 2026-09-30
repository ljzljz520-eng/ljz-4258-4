package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="pressure_mapping_revisions")
public class PressureMappingRevision {
 @Id private UUID id;
 @Column(name="old_mapping_id") private UUID oldMappingId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="new_mapping_id") private PressureCalibrationMapping newMapping;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="affected_segment_id") private BatchSegment affectedSegment;
 @Column(name="correction_level",nullable=false) private int correctionLevel;
 @Column(nullable=false) private String reason;
 @Column(name="created_by",nullable=false) private String createdBy;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 @PrePersist void assign(){if(id==null)id=UUID.randomUUID();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public UUID getOldMappingId(){return oldMappingId;} public void setOldMappingId(UUID v){oldMappingId=v;}
 public PressureCalibrationMapping getNewMapping(){return newMapping;} public void setNewMapping(PressureCalibrationMapping v){newMapping=v;}
 public BatchSegment getAffectedSegment(){return affectedSegment;} public void setAffectedSegment(BatchSegment v){affectedSegment=v;}
 public int getCorrectionLevel(){return correctionLevel;} public void setCorrectionLevel(int v){correctionLevel=v;}
 public String getReason(){return reason;} public void setReason(String v){reason=v;}
 public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
