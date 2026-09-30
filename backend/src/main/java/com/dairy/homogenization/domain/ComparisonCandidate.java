package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name = "comparison_candidates")
public class ComparisonCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "before_sample_id", nullable = false) private Long beforeSampleId;
    @Column(name = "after_sample_id", nullable = false) private Long afterSampleId;
    @Column(name = "batch_id", nullable = false) private Long batchId;
    @Column(name = "valve_group_id", nullable = false) private Long valveGroupId;
    @Column(name = "stage_code") private String stageCode;
    @Column(name = "before_pressure_mapping_version_id") private Long beforePressureMappingVersionId;
    @Column(name = "after_pressure_mapping_version_id") private Long afterPressureMappingVersionId;
    @Column(name = "before_algorithm_version_id") private Long beforeAlgorithmVersionId;
    @Column(name = "after_algorithm_version_id") private Long afterAlgorithmVersionId;
    @Column(name = "before_measurement_id") private Long beforeMeasurementId;
    @Column(name = "after_measurement_id") private Long afterMeasurementId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ComparisonStatus status = ComparisonStatus.CANDIDATE;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ComparisonOrigin origin = ComparisonOrigin.AUTO;
    @Column(nullable = false) private boolean comparable;
    @Column(name = "reason_summary") private String reasonSummary;
    private String fingerprint;
    @Version private Integer version;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private OffsetDateTime updatedAt;
    @OneToMany(mappedBy = "comparisonId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComparabilityFinding> findings = new ArrayList<>();
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getBeforeSampleId() { return beforeSampleId; } public void setBeforeSampleId(Long v) { this.beforeSampleId = v; }
    public Long getAfterSampleId() { return afterSampleId; } public void setAfterSampleId(Long v) { this.afterSampleId = v; }
    public Long getBatchId() { return batchId; } public void setBatchId(Long v) { this.batchId = v; }
    public Long getValveGroupId() { return valveGroupId; } public void setValveGroupId(Long v) { this.valveGroupId = v; }
    public String getStageCode() { return stageCode; } public void setStageCode(String v) { this.stageCode = v; }
    public Long getBeforePressureMappingVersionId() { return beforePressureMappingVersionId; } public void setBeforePressureMappingVersionId(Long v) { this.beforePressureMappingVersionId = v; }
    public Long getAfterPressureMappingVersionId() { return afterPressureMappingVersionId; } public void setAfterPressureMappingVersionId(Long v) { this.afterPressureMappingVersionId = v; }
    public Long getBeforeAlgorithmVersionId() { return beforeAlgorithmVersionId; } public void setBeforeAlgorithmVersionId(Long v) { this.beforeAlgorithmVersionId = v; }
    public Long getAfterAlgorithmVersionId() { return afterAlgorithmVersionId; } public void setAfterAlgorithmVersionId(Long v) { this.afterAlgorithmVersionId = v; }
    public Long getBeforeMeasurementId() { return beforeMeasurementId; } public void setBeforeMeasurementId(Long v) { this.beforeMeasurementId = v; }
    public Long getAfterMeasurementId() { return afterMeasurementId; } public void setAfterMeasurementId(Long v) { this.afterMeasurementId = v; }
    public ComparisonStatus getStatus() { return status; } public void setStatus(ComparisonStatus v) { this.status = v; }
    public ComparisonOrigin getOrigin() { return origin; } public void setOrigin(ComparisonOrigin v) { this.origin = v; }
    public boolean isComparable() { return comparable; } public void setComparable(boolean v) { this.comparable = v; }
    public String getReasonSummary() { return reasonSummary; } public void setReasonSummary(String v) { this.reasonSummary = v; }
    public String getFingerprint() { return fingerprint; } public void setFingerprint(String v) { this.fingerprint = v; }
    public Integer getVersion() { return version; } public void setVersion(Integer version) { this.version = version; }
    public List<ComparabilityFinding> getFindings() { return findings; }
}
