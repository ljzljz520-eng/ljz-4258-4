package com.example.dairy.service;

import com.example.dairy.domain.*;
import com.example.dairy.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class ImportService {
    private final BatchSegmentRepository segments;
    private final ImportJobRepository jobs;
    private final ImportFileRepository files;
    private final SampleRepository samples;
    private final MeasurementPointRepository points;
    private final AlgorithmVersionRepository algorithms;
    private final PressureCalibrationMappingRepository mappings;
    private final FileImportProcessor processor;

    public ImportService(BatchSegmentRepository segments, ImportJobRepository jobs, ImportFileRepository files,
                         SampleRepository samples, MeasurementPointRepository points,
                         AlgorithmVersionRepository algorithms, PressureCalibrationMappingRepository mappings,
                         FileImportProcessor processor) {
        this.segments = segments; this.jobs = jobs; this.files = files; this.samples = samples;
        this.points = points; this.algorithms = algorithms; this.mappings = mappings; this.processor = processor;
    }

    @Transactional
    public ImportJob createJob(UUID segmentId, List<MultipartFile> uploads, String actor) {
        if (uploads == null || uploads.isEmpty()) throw new ValidationException("至少上传一个仪器文件。");
        BatchSegment segment = segments.findById(segmentId).orElseThrow(() -> new NotFoundException("批段不存在"));
        if (segment.getStatus() != Enums.SegmentStatus.OPEN)
            throw new ConflictException("批段已锁定或签发：锁证/签发后不能再导入证据。");
        ImportJob job = new ImportJob();
        job.setId(UUID.randomUUID()); job.setSegment(segment); job.setStatus(Enums.ImportStatus.PENDING);
        job.setTotalFiles(uploads.size()); job.setCreatedBy(actor);
        java.util.Set<String> checksumsInUpload = new java.util.HashSet<>();
        for (MultipartFile upload : uploads) {
            try {
                byte[] content = upload.getBytes();
                String checksum = sha256(content);
                if (!checksumsInUpload.add(checksum))
                    throw new ValidationException("同一次上传包含重复文件校验和: " + upload.getOriginalFilename());
                if (files.existsByChecksumAndStatus(checksum, Enums.FileStatus.SUCCEEDED))
                    throw new ValidationException("文件 %s 的校验和已成功导入，拒绝重复证据。".formatted(upload.getOriginalFilename()));
                ImportFile file = new ImportFile();
                file.setId(UUID.randomUUID()); file.setFilename(upload.getOriginalFilename());
                file.setChecksum(checksum); file.setContent(content); file.setStatus(Enums.FileStatus.PENDING);
                job.addFile(file);
            } catch (IOException e) {
                throw new ValidationException("无法读取上传文件: " + e.getMessage());
            }
        }
        return jobs.save(job);
    }

    @Transactional
    public int processPendingJobs(int maxJobs, String workerName) {
        Instant staleAt = Instant.now().minusSeconds(60);
        List<ImportJob> claimable = jobs.findClaimable(staleAt, org.springframework.data.domain.PageRequest.of(0,maxJobs));
        int processed = 0;
        for (ImportJob locked : claimable) {
            ImportJob job = jobs.lockById(locked.getId()).orElseThrow();
            if (!canClaim(job, staleAt)) continue;
            job.setStatus(Enums.ImportStatus.RUNNING); job.setStartedAt(Instant.now());
            job.setLockedAt(Instant.now()); job.setLockedBy(workerName);
            for (ImportFile file : job.getFiles()) {
                if (file.getStatus() == Enums.FileStatus.SUCCEEDED) continue;
                boolean ok = false;
                String error = null;
                try {
                    processor.processFile(file.getId());
                    ok = true;
                } catch (RuntimeException ex) {
                    error = ex.getMessage();
                }
                file.setAttempts(file.getAttempts() + 1);
                if (ok) { file.setStatus(Enums.FileStatus.SUCCEEDED); file.setErrorMessage(null); file.setProcessedAt(Instant.now()); }
                else { file.setStatus(Enums.FileStatus.FAILED); file.setErrorMessage(truncate(error)); }
            }
            int success = (int) job.getFiles().stream().filter(f -> f.getStatus()==Enums.FileStatus.SUCCEEDED).count();
            int failed = job.getTotalFiles() - success;
            job.setSuccessfulFiles(success); job.setFailedFiles(failed); job.setFinishedAt(Instant.now());
            job.setLockedAt(null); job.setLockedBy(null);
            job.setStatus(failed == 0 ? Enums.ImportStatus.SUCCEEDED
                    : success > 0 ? Enums.ImportStatus.COMPLETED_WITH_FAILURES : Enums.ImportStatus.FAILED);
            processed++;
        }
        return processed;
    }

    private boolean canClaim(ImportJob job, Instant staleAt) {
        return job.getStatus()==Enums.ImportStatus.PENDING || job.getStatus()==Enums.ImportStatus.FAILED
                || job.getStatus()==Enums.ImportStatus.COMPLETED_WITH_FAILURES
                || (job.getStatus()==Enums.ImportStatus.RUNNING && (job.getLockedAt()==null || job.getLockedAt().isBefore(staleAt)));
    }

    @Transactional(readOnly=true)
    public ImportJob getJob(UUID jobId) { return jobs.findById(jobId).orElseThrow(() -> new NotFoundException("导入任务不存在")); }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append("%02x".formatted(b));
            return "sha256:" + sb;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String truncate(String s) { return s == null ? "未知错误" : (s.length() > 1000 ? s.substring(0,1000) : s); }
}
