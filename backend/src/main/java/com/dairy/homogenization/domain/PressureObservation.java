package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "pressure_observations")
public class PressureObservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_id", nullable = false) private Long batchId;
    @Column(name = "valve_group_id", nullable = false) private Long valveGroupId;
    @Column(name = "raw_label", nullable = false) private String rawLabel;
    @Column(name = "calibrated_stage_code") private String calibratedStageCode;
    @Column(name = "pressure_mapping_version_id") private Long pressureMappingVersionId;
    @Column(name = "observed_pressure_bar", precision = 12, scale = 4) private BigDecimal observedPressureBar;
    @Column(name = "observed_at", nullable = false) private OffsetDateTime observedAt;
    @Column(name = "import_file_id") private Long importFileId;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; } public void setBatchId(Long v) { this.batchId = v; }
    public Long getValveGroupId() { return valveGroupId; } public void setValveGroupId(Long v) { this.valveGroupId = v; }
    public String getRawLabel() { return rawLabel; } public void setRawLabel(String v) { this.rawLabel = v; }
    public String getCalibratedStageCode() { return calibratedStageCode; } public void setCalibratedStageCode(String v) { this.calibratedStageCode = v; }
    public Long getPressureMappingVersionId() { return pressureMappingVersionId; } public void setPressureMappingVersionId(Long v) { this.pressureMappingVersionId = v; }
    public BigDecimal getObservedPressureBar() { return observedPressureBar; } public void setObservedPressureBar(BigDecimal v) { this.observedPressureBar = v; }
    public OffsetDateTime getObservedAt() { return observedAt; } public void setObservedAt(OffsetDateTime v) { this.observedAt = v; }
    public Long getImportFileId() { return importFileId; } public void setImportFileId(Long v) { this.importFileId = v; }
}
