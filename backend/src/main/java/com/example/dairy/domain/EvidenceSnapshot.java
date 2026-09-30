package com.example.dairy.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant; import java.util.UUID;
@Entity @Table(name="evidence_snapshots")
public class EvidenceSnapshot {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="segment_id") private BatchSegment segment;
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private String payload;
 @Column(nullable=false,unique=true) private String sha256;
 @Column(name="created_by",nullable=false) private String createdBy;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 @PrePersist void assign(){if(id==null)id=UUID.randomUUID();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public BatchSegment getSegment(){return segment;} public void setSegment(BatchSegment v){segment=v;}
 public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
 public String getSha256(){return sha256;} public void setSha256(String v){sha256=v;}
 public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
