package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "instrument_algorithm_versions")
public class InstrumentAlgorithmVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "instrument_code", nullable = false) private String instrumentCode;
    @Column(name = "measurement_kind", nullable = false) private String measurementKind = "PARTICLE_SIZE";
    @Column(name = "algorithm_version", nullable = false) private String algorithmVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlgorithmStatus status = AlgorithmStatus.ACTIVE;
    @Column(name = "effective_from", nullable = false) private OffsetDateTime effectiveFrom;
    @Column(name = "effective_to") private OffsetDateTime effectiveTo;
    private String description;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getInstrumentCode() { return instrumentCode; } public void setInstrumentCode(String v) { this.instrumentCode = v; }
    public String getMeasurementKind() { return measurementKind; } public void setMeasurementKind(String v) { this.measurementKind = v; }
    public String getAlgorithmVersion() { return algorithmVersion; } public void setAlgorithmVersion(String v) { this.algorithmVersion = v; }
    public AlgorithmStatus getStatus() { return status; } public void setStatus(AlgorithmStatus v) { this.status = v; }
    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; } public void setEffectiveFrom(OffsetDateTime v) { this.effectiveFrom = v; }
    public OffsetDateTime getEffectiveTo() { return effectiveTo; } public void setEffectiveTo(OffsetDateTime v) { this.effectiveTo = v; }
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
}
