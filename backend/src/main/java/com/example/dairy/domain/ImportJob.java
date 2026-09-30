package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.ArrayList; import java.util.List; import java.util.UUID;
@Entity @Table(name="import_jobs")
public class ImportJob {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="segment_id") private BatchSegment segment;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private Enums.ImportStatus status=Enums.ImportStatus.PENDING;
 @Column(name="total_files",nullable=false) private int totalFiles;
 @Column(name="successful_files",nullable=false) private int successfulFiles;
 @Column(name="failed_files",nullable=false) private int failedFiles;
 @Column(name="created_by",nullable=false) private String createdBy;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 @Column(name="started_at") private Instant startedAt;
 @Column(name="finished_at") private Instant finishedAt;
 @Column(name="locked_at") private Instant lockedAt;
 @Column(name="locked_by") private String lockedBy;
 @OneToMany(mappedBy="job", cascade=CascadeType.ALL) private List<ImportFile> files = new ArrayList<>();
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public BatchSegment getSegment(){return segment;} public void setSegment(BatchSegment v){segment=v;}
 public Enums.ImportStatus getStatus(){return status;} public void setStatus(Enums.ImportStatus v){status=v;}
 public int getTotalFiles(){return totalFiles;} public void setTotalFiles(int v){totalFiles=v;}
 public int getSuccessfulFiles(){return successfulFiles;} public void setSuccessfulFiles(int v){successfulFiles=v;}
 public int getFailedFiles(){return failedFiles;} public void setFailedFiles(int v){failedFiles=v;}
 public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
 public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
 public Instant getFinishedAt(){return finishedAt;} public void setFinishedAt(Instant v){finishedAt=v;}
 public Instant getLockedAt(){return lockedAt;} public void setLockedAt(Instant v){lockedAt=v;}
 public String getLockedBy(){return lockedBy;} public void setLockedBy(String v){lockedBy=v;}
 public List<ImportFile> getFiles(){return files;}
 public void addFile(ImportFile f){files.add(f); f.setJob(this);}
}
