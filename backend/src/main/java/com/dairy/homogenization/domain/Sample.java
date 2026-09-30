package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "samples")
public class Sample {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "sample_code", nullable = false, unique = true) private String sampleCode;
    @Column(name = "batch_id", nullable = false) private Long batchId;
    @Column(name = "sample_point_id", nullable = false) private Long samplePointId;
    @Column(name = "valve_group_id", nullable = false) private Long valveGroupId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SamplePosition position;
    @Column(name = "layer_name", nullable = false) private String layerName;
    @Column(name = "sampled_at", nullable = false) private OffsetDateTime sampledAt;
    @Column(name = "received_at") private OffsetDateTime receivedAt;
    @Column(name = "transport_delay_seconds") private Integer transportDelaySeconds;
    @Column(name = "container_code") private String containerCode;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getSampleCode() { return sampleCode; } public void setSampleCode(String v) { this.sampleCode = v; }
    public Long getBatchId() { return batchId; } public void setBatchId(Long v) { this.batchId = v; }
    public Long getSamplePointId() { return samplePointId; } public void setSamplePointId(Long v) { this.samplePointId = v; }
    public Long getValveGroupId() { return valveGroupId; } public void setValveGroupId(Long v) { this.valveGroupId = v; }
    public SamplePosition getPosition() { return position; } public void setPosition(SamplePosition v) { this.position = v; }
    public String getLayerName() { return layerName; } public void setLayerName(String v) { this.layerName = v; }
    public OffsetDateTime getSampledAt() { return sampledAt; } public void setSampledAt(OffsetDateTime v) { this.sampledAt = v; }
    public OffsetDateTime getReceivedAt() { return receivedAt; } public void setReceivedAt(OffsetDateTime v) { this.receivedAt = v; }
    public Integer getTransportDelaySeconds() { return transportDelaySeconds; } public void setTransportDelaySeconds(Integer v) { this.transportDelaySeconds = v; }
    public String getContainerCode() { return containerCode; } public void setContainerCode(String v) { this.containerCode = v; }
}
