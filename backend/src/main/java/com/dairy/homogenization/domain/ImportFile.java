package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "import_files")
public class ImportFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "job_id", nullable = false) private Long jobId;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Enumerated(EnumType.STRING) @Column(name = "file_type", nullable = false) private FileType fileType;
    @Lob @Column(name = "content", nullable = false, columnDefinition = "text") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ImportFileStatus status = ImportFileStatus.PENDING;
    private int attempts;
    @Column(name = "error_message", length = 4000) private String errorMessage;
    @Column(name = "checksum_sha256", nullable = false) private String checksumSha256;
    @Column(name = "processed_at") private OffsetDateTime processedAt;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) private OffsetDateTime updatedAt;
    @Version private Integer version;
    public enum FileType { PRESSURE_CSV, PARTICLE_CSV }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; } public void setJobId(Long v) { this.jobId = v; }
    public String getFileName() { return fileName; } public void setFileName(String v) { this.fileName = v; }
    public FileType getFileType() { return fileType; } public void setFileType(FileType v) { this.fileType = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public ImportFileStatus getStatus() { return status; } public void setStatus(ImportFileStatus v) { this.status = v; }
    public int getAttempts() { return attempts; } public void setAttempts(int v) { this.attempts = v; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String v) { this.errorMessage = v; }
    public String getChecksumSha256() { return checksumSha256; } public void setChecksumSha256(String v) { this.checksumSha256 = v; }
    public OffsetDateTime getProcessedAt() { return processedAt; } public void setProcessedAt(OffsetDateTime v) { this.processedAt = v; }
    public Integer getVersion() { return version; } public void setVersion(Integer version) { this.version = version; }
}
