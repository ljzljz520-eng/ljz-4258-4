package com.example.dairy.domain;
import jakarta.persistence.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
import java.time.Instant; import java.util.UUID;
@Entity @Table(name="audit_logs")
public class AuditLog {
 @Id private UUID id;
 @Column(nullable=false) private String actor;
 @Column(nullable=false) private String action;
 @Column(name="segment_id") private UUID segmentId;
 @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private String detail="{}";
 @Column(name="created_at") private Instant createdAt=Instant.now();
 @PrePersist void assign(){if(id==null)id=UUID.randomUUID();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public String getActor(){return actor;} public void setActor(String v){actor=v;}
 public String getAction(){return action;} public void setAction(String v){action=v;}
 public UUID getSegmentId(){return segmentId;} public void setSegmentId(UUID v){segmentId=v;}
 public String getDetail(){return detail;} public void setDetail(String v){detail=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
