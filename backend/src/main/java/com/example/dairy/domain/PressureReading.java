package com.example.dairy.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="pressure_readings")
public class PressureReading {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sample_id") private Sample sample;
 @Column(name="stage_order",nullable=false) private int stageOrder;
 @Column(name="raw_label",nullable=false) private String rawLabel;
 @Column(name="raw_value_bar",nullable=false,precision=12,scale=4) private BigDecimal rawValueBar;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="mapping_id") private PressureCalibrationMapping mapping;
 @Column(name="corrected_value_bar",precision=12,scale=4) private BigDecimal correctedValueBar;
 @Column(name="measured_at",nullable=false) private Instant measuredAt;
 @PrePersist void assign(){ if(id==null) id=UUID.randomUUID(); }
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public Sample getSample(){return sample;} public void setSample(Sample v){sample=v;}
 public int getStageOrder(){return stageOrder;} public void setStageOrder(int v){stageOrder=v;}
 public String getRawLabel(){return rawLabel;} public void setRawLabel(String v){rawLabel=v;}
 public BigDecimal getRawValueBar(){return rawValueBar;} public void setRawValueBar(BigDecimal v){rawValueBar=v;}
 public PressureCalibrationMapping getMapping(){return mapping;} public void setMapping(PressureCalibrationMapping v){mapping=v;}
 public BigDecimal getCorrectedValueBar(){return correctedValueBar;} public void setCorrectedValueBar(BigDecimal v){correctedValueBar=v;}
 public Instant getMeasuredAt(){return measuredAt;} public void setMeasuredAt(Instant v){measuredAt=v;}
}
