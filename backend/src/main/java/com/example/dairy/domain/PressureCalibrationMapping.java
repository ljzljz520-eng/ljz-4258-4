package com.example.dairy.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="pressure_calibration_mappings")
public class PressureCalibrationMapping {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="valve_group_id") private ValveGroup valveGroup;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="measurement_point_id") private MeasurementPoint measurementPoint;
 @Column(name="instrument_label",nullable=false) private String instrumentLabel;
 @Column(name="canonical_stage_label",nullable=false) private String canonicalStageLabel;
 @Column(name="correction_level",nullable=false) private int correctionLevel;
 @Column(nullable=false) private int version;
 @Column(nullable=false,precision=12,scale=6) private BigDecimal slope = BigDecimal.ONE;
 @Column(nullable=false,precision=12,scale=6) private BigDecimal intercept = BigDecimal.ZERO;
 @Column(name="effective_from",nullable=false) private Instant effectiveFrom;
 private boolean active=true;
 @Column(name="supersedes_id") private UUID supersedesId;
 @Column(name="created_by",nullable=false) private String createdBy;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public ValveGroup getValveGroup(){return valveGroup;} public void setValveGroup(ValveGroup v){valveGroup=v;}
 public MeasurementPoint getMeasurementPoint(){return measurementPoint;} public void setMeasurementPoint(MeasurementPoint v){measurementPoint=v;}
 public String getInstrumentLabel(){return instrumentLabel;} public void setInstrumentLabel(String v){instrumentLabel=v;}
 public String getCanonicalStageLabel(){return canonicalStageLabel;} public void setCanonicalStageLabel(String v){canonicalStageLabel=v;}
 public int getCorrectionLevel(){return correctionLevel;} public void setCorrectionLevel(int v){correctionLevel=v;}
 public int getVersion(){return version;} public void setVersion(int v){version=v;}
 public BigDecimal getSlope(){return slope;} public void setSlope(BigDecimal v){slope=v;}
 public BigDecimal getIntercept(){return intercept;} public void setIntercept(BigDecimal v){intercept=v;}
 public Instant getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(Instant v){effectiveFrom=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public UUID getSupersedesId(){return supersedesId;} public void setSupersedesId(UUID v){supersedesId=v;}
 public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
