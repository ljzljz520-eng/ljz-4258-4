package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "pressure_mapping_items")
public class PressureMappingItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private PressureMappingVersion pressureMappingVersion;
    @Column(name = "raw_label", nullable = false) private String rawLabel;
    @Column(name = "calibrated_stage_code", nullable = false) private String calibratedStageCode;
    @Column(name = "nominal_pressure_bar", precision = 10, scale = 3) private BigDecimal nominalPressureBar;
    @Column(name = "display_order") private int displayOrder;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public PressureMappingVersion getPressureMappingVersion() { return pressureMappingVersion; }
    public void setPressureMappingVersion(PressureMappingVersion v) { this.pressureMappingVersion = v; }
    public String getRawLabel() { return rawLabel; } public void setRawLabel(String v) { this.rawLabel = v; }
    public String getCalibratedStageCode() { return calibratedStageCode; } public void setCalibratedStageCode(String v) { this.calibratedStageCode = v; }
    public BigDecimal getNominalPressureBar() { return nominalPressureBar; } public void setNominalPressureBar(BigDecimal v) { this.nominalPressureBar = v; }
    public int getDisplayOrder() { return displayOrder; } public void setDisplayOrder(int v) { this.displayOrder = v; }
}
