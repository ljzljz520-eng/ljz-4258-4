package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "sample_points")
public class SamplePoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "valve_group_id", nullable = false) private Long valveGroupId;
    @Column(name = "sampling_line", nullable = false) private String samplingLine;
    @Column(name = "product_flow_order") private int productFlowOrder;
    private boolean active = true;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public Long getValveGroupId() { return valveGroupId; } public void setValveGroupId(Long v) { this.valveGroupId = v; }
    public String getSamplingLine() { return samplingLine; } public void setSamplingLine(String s) { this.samplingLine = s; }
    public int getProductFlowOrder() { return productFlowOrder; } public void setProductFlowOrder(int v) { this.productFlowOrder = v; }
    public boolean isActive() { return active; } public void setActive(boolean active) { this.active = active; }
}
