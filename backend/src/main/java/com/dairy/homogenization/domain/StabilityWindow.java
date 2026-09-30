package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "stability_windows")
public class StabilityWindow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_id", nullable = false) private Long batchId;
    @Column(name = "valve_group_id", nullable = false) private Long valveGroupId;
    @Column(name = "stage_code", nullable = false) private String stageCode;
    @Column(name = "started_at", nullable = false) private OffsetDateTime startedAt;
    @Column(name = "ended_at", nullable = false) private OffsetDateTime endedAt;
    @Column(name = "nominal_pressure_bar", precision = 10, scale = 3) private BigDecimal nominalPressureBar;
    private String notes;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; } public void setBatchId(Long v) { this.batchId = v; }
    public Long getValveGroupId() { return valveGroupId; } public void setValveGroupId(Long v) { this.valveGroupId = v; }
    public String getStageCode() { return stageCode; } public void setStageCode(String v) { this.stageCode = v; }
    public OffsetDateTime getStartedAt() { return startedAt; } public void setStartedAt(OffsetDateTime v) { this.startedAt = v; }
    public OffsetDateTime getEndedAt() { return endedAt; } public void setEndedAt(OffsetDateTime v) { this.endedAt = v; }
    public BigDecimal getNominalPressureBar() { return nominalPressureBar; } public void setNominalPressureBar(BigDecimal v) { this.nominalPressureBar = v; }
    public String getNotes() { return notes; } public void setNotes(String v) { this.notes = v; }
}
