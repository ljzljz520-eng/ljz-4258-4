package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name = "particle_measurements")
public class ParticleMeasurement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "sample_id", nullable = false) private Long sampleId;
    @Column(name = "algorithm_version_id", nullable = false) private Long algorithmVersionId;
    @Column(name = "import_file_id") private Long importFileId;
    @Column(name = "d10_um", precision = 12, scale = 4) private BigDecimal d10Um;
    @Column(name = "d50_um", precision = 12, scale = 4) private BigDecimal d50Um;
    @Column(name = "d90_um", precision = 12, scale = 4) private BigDecimal d90Um;
    @Column(name = "mean_um", precision = 12, scale = 4) private BigDecimal meanUm;
    @Column(name = "measured_at", nullable = false) private OffsetDateTime measuredAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MeasurementStatus status = MeasurementStatus.ACTIVE;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @OneToMany(mappedBy = "particleMeasurement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<SizeDistributionPoint> distribution = new ArrayList<>();
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getSampleId() { return sampleId; } public void setSampleId(Long v) { this.sampleId = v; }
    public Long getAlgorithmVersionId() { return algorithmVersionId; } public void setAlgorithmVersionId(Long v) { this.algorithmVersionId = v; }
    public Long getImportFileId() { return importFileId; } public void setImportFileId(Long v) { this.importFileId = v; }
    public BigDecimal getD10Um() { return d10Um; } public void setD10Um(BigDecimal v) { this.d10Um = v; }
    public BigDecimal getD50Um() { return d50Um; } public void setD50Um(BigDecimal v) { this.d50Um = v; }
    public BigDecimal getD90Um() { return d90Um; } public void setD90Um(BigDecimal v) { this.d90Um = v; }
    public BigDecimal getMeanUm() { return meanUm; } public void setMeanUm(BigDecimal v) { this.meanUm = v; }
    public OffsetDateTime getMeasuredAt() { return measuredAt; } public void setMeasuredAt(OffsetDateTime v) { this.measuredAt = v; }
    public MeasurementStatus getStatus() { return status; } public void setStatus(MeasurementStatus v) { this.status = v; }
    public List<SizeDistributionPoint> getDistribution() { return distribution; }
}
