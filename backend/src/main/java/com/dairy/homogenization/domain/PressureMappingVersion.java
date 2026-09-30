package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name = "pressure_mapping_versions")
public class PressureMappingVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "valve_group_id", nullable = false) private Long valveGroupId;
    @Column(name = "version", nullable = false) private Integer versionNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MappingStatus status = MappingStatus.DRAFT;
    @Column(name = "effective_from", nullable = false) private OffsetDateTime effectiveFrom;
    @Column(name = "effective_to") private OffsetDateTime effectiveTo;
    @Column(name = "corrected_by") private String correctedBy;
    @Column(name = "corrected_at") private OffsetDateTime correctedAt;
    @Column(name = "change_summary", nullable = false) private String changeSummary;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @OneToMany(mappedBy = "pressureMappingVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PressureMappingItem> items = new ArrayList<>();
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getValveGroupId() { return valveGroupId; } public void setValveGroupId(Long v) { this.valveGroupId = v; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer v) { this.versionNumber = v; }
    public MappingStatus getStatus() { return status; } public void setStatus(MappingStatus v) { this.status = v; }
    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; } public void setEffectiveFrom(OffsetDateTime v) { this.effectiveFrom = v; }
    public OffsetDateTime getEffectiveTo() { return effectiveTo; } public void setEffectiveTo(OffsetDateTime v) { this.effectiveTo = v; }
    public String getCorrectedBy() { return correctedBy; } public void setCorrectedBy(String v) { this.correctedBy = v; }
    public OffsetDateTime getCorrectedAt() { return correctedAt; } public void setCorrectedAt(OffsetDateTime v) { this.correctedAt = v; }
    public String getChangeSummary() { return changeSummary; } public void setChangeSummary(String v) { this.changeSummary = v; }
    public List<PressureMappingItem> getItems() { return items; }
}
