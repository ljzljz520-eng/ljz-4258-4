package com.example.dairy.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "batch_segments")
public class BatchSegment {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="valve_group_id") private ValveGroup valveGroup;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sampling_line_id") private SamplingLine samplingLine;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_batch_id") private ProductBatch productBatch;
    @Column(nullable=false,unique=true) private String code;
    @Column(name="target_stage_label",nullable=false) private String targetStageLabel;
    @Column(name="baseline_start",nullable=false) private Instant baselineStart;
    @Column(name="baseline_end",nullable=false) private Instant baselineEnd;
    @Column(name="stable_start",nullable=false) private Instant stableStart;
    @Column(name="stable_end",nullable=false) private Instant stableEnd;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Enums.SegmentStatus status = Enums.SegmentStatus.OPEN;
    @Version private long lockVersion;
    @Column(name="created_at") private Instant createdAt = Instant.now();
    public UUID getId(){return id;} public void setId(UUID v){id=v;}
    public ValveGroup getValveGroup(){return valveGroup;} public void setValveGroup(ValveGroup v){valveGroup=v;}
    public SamplingLine getSamplingLine(){return samplingLine;} public void setSamplingLine(SamplingLine v){samplingLine=v;}
    public ProductBatch getProductBatch(){return productBatch;} public void setProductBatch(ProductBatch v){productBatch=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getTargetStageLabel(){return targetStageLabel;} public void setTargetStageLabel(String v){targetStageLabel=v;}
    public Instant getBaselineStart(){return baselineStart;} public void setBaselineStart(Instant v){baselineStart=v;}
    public Instant getBaselineEnd(){return baselineEnd;} public void setBaselineEnd(Instant v){baselineEnd=v;}
    public Instant getStableStart(){return stableStart;} public void setStableStart(Instant v){stableStart=v;}
    public Instant getStableEnd(){return stableEnd;} public void setStableEnd(Instant v){stableEnd=v;}
    public Enums.SegmentStatus getStatus(){return status;} public void setStatus(Enums.SegmentStatus v){status=v;}
    public long getLockVersion(){return lockVersion;} public void setLockVersion(long v){lockVersion=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
