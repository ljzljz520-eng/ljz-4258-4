package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "import_jobs")
public class ImportJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "external_job_id", nullable = false, unique = true) private String externalJobId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ImportJobStatus status = ImportJobStatus.RECEIVED;
    @Column(name = "uploaded_by", nullable = false) private String uploadedBy;
    @Column(name = "total_files") private int totalFiles;
    @Column(name = "succeeded_files") private int succeededFiles;
    @Column(name = "failed_files") private int failedFiles;
    @Column(name = "error_summary", length = 4000) private String errorSummary;
    @Column(name = "claimed_at") private OffsetDateTime claimedAt;
    @Column(name = "finished_at") private OffsetDateTime finishedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private OffsetDateTime updatedAt;
    @Version private Integer version;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getExternalJobId() { return externalJobId; } public void setExternalJobId(String v) { this.externalJobId = v; }
    public ImportJobStatus getStatus() { return status; } public void setStatus(ImportJobStatus v) { this.status = v; }
    public String getUploadedBy() { return uploadedBy; } public void setUploadedBy(String v) { this.uploadedBy = v; }
    public int getTotalFiles() { return totalFiles; } public void setTotalFiles(int v) { this.totalFiles = v; }
    public int getSucceededFiles() { return succeededFiles; } public void setSucceededFiles(int v) { this.succeededFiles = v; }
    public int getFailedFiles() { return failedFiles; } public void setFailedFiles(int v) { this.failedFiles = v; }
    public String getErrorSummary() { return errorSummary; } public void setErrorSummary(String v) { this.errorSummary = v; }
    public OffsetDateTime getClaimedAt() { return claimedAt; } public void setClaimedAt(OffsetDateTime v) { this.claimedAt = v; }
    public OffsetDateTime getFinishedAt() { return finishedAt; } public void setFinishedAt(OffsetDateTime v) { this.finishedAt = v; }
    public Integer getVersion() { return version; } public void setVersion(Integer version) { this.version = version; }
}
