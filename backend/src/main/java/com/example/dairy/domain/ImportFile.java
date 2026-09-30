package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="import_files")
public class ImportFile {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="job_id") private ImportJob job;
 @Column(nullable=false) private String filename;
 @Column(nullable=false) private String checksum;
 @Column(nullable=false) private byte[] content;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Enums.FileStatus status=Enums.FileStatus.PENDING;
 @Column(nullable=false) private int attempts;
 @Column(name="error_message") private String errorMessage;
 @Column(name="locked_at") private Instant lockedAt;
 @Column(name="locked_by") private String lockedBy;
 @Column(name="processed_at") private Instant processedAt;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public ImportJob getJob(){return job;} public void setJob(ImportJob v){job=v;}
 public String getFilename(){return filename;} public void setFilename(String v){filename=v;}
 public String getChecksum(){return checksum;} public void setChecksum(String v){checksum=v;}
 public byte[] getContent(){return content;} public void setContent(byte[] v){content=v;}
 public Enums.FileStatus getStatus(){return status;} public void setStatus(Enums.FileStatus v){status=v;}
 public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}
 public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
 public Instant getLockedAt(){return lockedAt;} public void setLockedAt(Instant v){lockedAt=v;}
 public String getLockedBy(){return lockedBy;} public void setLockedBy(String v){lockedBy=v;}
 public Instant getProcessedAt(){return processedAt;} public void setProcessedAt(Instant v){processedAt=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
