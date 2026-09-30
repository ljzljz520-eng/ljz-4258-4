package com.example.dairy.domain;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="comparison_reasons")
public class ComparisonReason {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="comparison_id") private Comparison comparison;
 @Column(nullable=false) private String code;
 @Column(nullable=false) private String detail;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private Enums.Severity severity=Enums.Severity.BLOCKER;
 @PrePersist void assign(){if(id==null)id=UUID.randomUUID();}
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public Comparison getComparison(){return comparison;} public void setComparison(Comparison v){comparison=v;}
 public String getCode(){return code;} public void setCode(String v){code=v;}
 public String getDetail(){return detail;} public void setDetail(String v){detail=v;}
 public Enums.Severity getSeverity(){return severity;} public void setSeverity(Enums.Severity v){severity=v;}
}
