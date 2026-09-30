package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ImportService {
    private final ImportJobRepository jobs;
    private final ImportFileRepository files;
    private final ProductBatchRepository batches;
    private final ValveGroupRepository valveGroups;
    private final SamplePointRepository points;
    private final PressureMappingVersionRepository mappings;
    private final PressureObservationRepository observations;
    private final InstrumentAlgorithmVersionRepository algorithms;
    private final SampleRepository samples;
    private final ParticleMeasurementRepository measurements;
    private final InstrumentCsvParser parser;
    private final ComparisonRuleApplicationService comparisonRules;
    private final TransactionTemplate transactionTemplate;

    public ImportService(ImportJobRepository jobs, ImportFileRepository files, ProductBatchRepository batches,
                         ValveGroupRepository valveGroups, SamplePointRepository points,
                         PressureMappingVersionRepository mappings, PressureObservationRepository observations,
                         InstrumentAlgorithmVersionRepository algorithms, SampleRepository samples,
                         ParticleMeasurementRepository measurements, InstrumentCsvParser parser,
                         ComparisonRuleApplicationService comparisonRules,
                         org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.jobs = jobs; this.files = files; this.batches = batches; this.valveGroups = valveGroups;
        this.points = points; this.mappings = mappings; this.observations = observations;
        this.algorithms = algorithms; this.samples = samples; this.measurements = measurements;
        this.parser = parser; this.comparisonRules = comparisonRules;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public ImportJob createJob(List<MultipartFile> uploads, String type, String user) {
        if (!jobs.findActive().isEmpty())
            throw new IllegalStateException("已有仪器导入任务在接收或处理中；请等待处理/重试结束，避免导入途中签发。");
        ImportJob job = new ImportJob();
        job.setExternalJobId(UUID.randomUUID().toString());
        job.setStatus(ImportJobStatus.RECEIVED);
        job.setUploadedBy(user);
        job.setTotalFiles(uploads.size());
        job = jobs.save(job);
        ImportFile.FileType fileType = ImportFile.FileType.valueOf(type);
        for (MultipartFile upload : uploads) {
            try {
                String content = new String(upload.getBytes(), StandardCharsets.UTF_8);
                String sha = sha256(content);
                files.findByChecksumSha256AndStatus(sha, ImportFileStatus.SUCCESS).ifPresent(f -> {
                    throw new IllegalArgumentException("文件已成功导入，不能重复形成证据: " + f.getFileName());
                });
                ImportFile f = new ImportFile();
                f.setJobId(job.getId()); f.setFileName(upload.getOriginalFilename());
                f.setFileType(fileType); f.setContent(content); f.setChecksumSha256(sha);
                f.setStatus(ImportFileStatus.PENDING);
                files.save(f);
            } catch (IOException e) {
                throw new IllegalArgumentException("无法读取文件 " + upload.getOriginalFilename(), e);
            }
        }
        return job;
    }

    public void processNextJob() {
        List<ImportJob> active = jobs.findActive();
        if (active.isEmpty()) return;
        ImportJob job = active.get(0);
        if (job.getStatus() == ImportJobStatus.RECEIVED) {
            Long jobId = job.getId();
            transactionTemplate.executeWithoutResult(status -> {
                ImportJob fresh = jobs.findById(jobId).orElseThrow();
                fresh.setStatus(ImportJobStatus.PROCESSING);
                fresh.setClaimedAt(OffsetDateTime.now());
            });
        }
        List<ImportFile> pending = files.findByJobIdAndStatusInOrderByIdAsc(job.getId(),
                List.of(ImportFileStatus.PENDING, ImportFileStatus.RETRYING));
        List<String> errors = new ArrayList<>();
        for (ImportFile file : pending) {
            try {
                Long currentFileId = file.getId();
                transactionTemplate.executeWithoutResult(status -> processFileInNewTransaction(currentFileId));
            } catch (Exception e) {
                errors.add(file.getFileName() + ": " + rootMessage(e));
                markFileFailed(file.getId(), rootMessage(e));
            }
        }
        Long jobIdForStatus = job.getId();
        transactionTemplate.executeWithoutResult(status -> recalculateJob(jobIdForStatus, errors));
    }

    public ImportFile retryFile(Long fileId) {
        ImportFile file = files.findById(fileId).orElseThrow(() -> new IllegalArgumentException("文件不存在"));
        ImportJob job = jobs.findById(file.getJobId()).orElseThrow();
        Long jobId = job.getId();
        transactionTemplate.executeWithoutResult(status -> {
            ImportFile fresh = files.findById(fileId).orElseThrow();
            fresh.setStatus(ImportFileStatus.RETRYING);
            fresh.setErrorMessage(null);
            ImportJob freshJob = jobs.findById(jobId).orElseThrow();
            freshJob.setStatus(ImportJobStatus.PROCESSING);
        });
        try {
            transactionTemplate.executeWithoutResult(status -> processFileInNewTransaction(fileId));
        } catch (Exception e) {
            markFileFailed(fileId, rootMessage(e));
        }
        Long jobIdForStatus = job.getId();
        transactionTemplate.executeWithoutResult(status -> recalculateJob(jobIdForStatus, List.of()));
        return files.findById(fileId).orElseThrow();
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processFileInNewTransaction(Long fileId) {
        ImportFile file = files.findById(fileId).orElseThrow();
        file.setStatus(ImportFileStatus.PROCESSING);
        file.setAttempts(file.getAttempts() + 1);
        files.save(file);
        if (file.getFileType() == ImportFile.FileType.PRESSURE_CSV) processPressure(file);
        else processParticle(file);
        file.setStatus(ImportFileStatus.SUCCESS);
        file.setErrorMessage(null);
        file.setProcessedAt(OffsetDateTime.now());
        files.save(file);
    }

    private void markFileFailed(Long id, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            ImportFile f = files.findById(id).orElseThrow();
            f.setStatus(ImportFileStatus.FAILED);
            f.setErrorMessage(message);
            files.save(f);
        });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recalculateJob(Long jobId, List<String> initialErrors) {
        ImportJob job = jobs.findById(jobId).orElseThrow();
        List<ImportFile> all = files.findByJobIdOrderByIdAsc(jobId);
        int success = (int) all.stream().filter(f -> f.getStatus() == ImportFileStatus.SUCCESS).count();
        int failed = (int) all.stream().filter(f -> f.getStatus() == ImportFileStatus.FAILED).count();
        int pending = all.size() - success - failed;
        job.setSucceededFiles(success); job.setFailedFiles(failed);
        List<String> errors = new ArrayList<>(initialErrors);
        all.stream().filter(f -> f.getStatus() == ImportFileStatus.FAILED).map(f -> f.getFileName()+": "+f.getErrorMessage()).forEach(errors::add);
        job.setErrorSummary(String.join(" | ", errors.stream().distinct().limit(20).toList()));
        if (pending == 0) {
            job.setStatus(failed == 0 ? ImportJobStatus.COMPLETED : ImportJobStatus.PARTIAL_SUCCESS);
            job.setFinishedAt(OffsetDateTime.now());
            } else job.setStatus(ImportJobStatus.PROCESSING);
        jobs.save(job);
    }

    private void processPressure(ImportFile file) {
        var rows = parser.parsePressure(file.getContent());
        Set<Long> affectedBatches = new HashSet<>();
        for (var row : rows) {
            ProductBatch batch = batches.findByBatchNumber(row.batchNumber())
                    .orElseThrow(() -> new IllegalArgumentException("产品批不存在: " + row.batchNumber()));
            ValveGroup vg = valveGroups.findByCode(row.valveGroupCode())
                    .orElseThrow(() -> new IllegalArgumentException("阀组不存在: " + row.valveGroupCode()));
            PressureMappingVersion mapping = mappings.findByValveGroupIdAndStatus(vg.getId(), MappingStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalStateException("阀组缺少 ACTIVE 压力映射"));
            String stage = mapping.getItems().stream()
                    .filter(i -> i.getRawLabel().equals(row.rawLabel())).map(PressureMappingItem::getCalibratedStageCode)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("原始标签未校准: " + row.rawLabel()));
            PressureObservation o = new PressureObservation();
            o.setBatchId(batch.getId()); o.setValveGroupId(vg.getId()); o.setRawLabel(row.rawLabel());
            o.setCalibratedStageCode(stage); o.setPressureMappingVersionId(mapping.getId());
            o.setObservedPressureBar(row.pressureBar()); o.setObservedAt(row.observedAt());
            o.setImportFileId(file.getId());
            observations.save(o);
            affectedBatches.add(batch.getId());
        }
    }

    private void processParticle(ImportFile file) {
        var rows = parser.parseParticle(file.getContent());
        Set<Long> affectedBatches = new HashSet<>();
        for (var row : rows) {
            ProductBatch batch = batches.findByBatchNumber(row.batchNumber())
                    .orElseThrow(() -> new IllegalArgumentException("产品批不存在: " + row.batchNumber()));
            SamplePoint point = points.findByCode(row.samplePointCode())
                    .orElseThrow(() -> new IllegalArgumentException("取样点不存在: " + row.samplePointCode()));
            Sample sample = samples.findBySampleCode(row.sampleCode()).orElseGet(() -> {
                Sample s = new Sample();
                s.setSampleCode(row.sampleCode()); s.setBatchId(batch.getId());
                s.setSamplePointId(point.getId()); s.setValveGroupId(point.getValveGroupId());
                s.setPosition(SamplePosition.valueOf(row.position())); s.setLayerName(row.layer());
                s.setSampledAt(row.sampledAt()); s.setReceivedAt(row.receivedAt());
                if (row.receivedAt() != null) s.setTransportDelaySeconds((int) java.time.Duration.between(row.sampledAt(), row.receivedAt()).getSeconds());
                return samples.save(s);
            });
            if (!Objects.equals(sample.getBatchId(), batch.getId()) || !Objects.equals(sample.getSamplePointId(), point.getId()))
                throw new IllegalArgumentException("样品编码已关联到其他批/取样点: " + row.sampleCode());
            sample.setReceivedAt(row.receivedAt());
            if (row.receivedAt() != null) sample.setTransportDelaySeconds((int) java.time.Duration.between(row.sampledAt(), row.receivedAt()).getSeconds());
            InstrumentAlgorithmVersion algorithm = algorithms
                    .findByInstrumentCodeAndMeasurementKindAndAlgorithmVersion(row.instrumentCode(), "PARTICLE_SIZE", row.algorithmVersion())
                    .orElseThrow(() -> new IllegalArgumentException("粒度算法版本未登记: " + row.algorithmVersion()));
            measurements.findBySampleIdAndStatus(sample.getId(), MeasurementStatus.ACTIVE).ifPresent(old -> {
                old.setStatus(MeasurementStatus.SUPERSEDED);
                measurements.save(old);
            });
            ParticleMeasurement m = new ParticleMeasurement();
            m.setSampleId(sample.getId()); m.setAlgorithmVersionId(algorithm.getId()); m.setImportFileId(file.getId());
            m.setD10Um(row.d10()); m.setD50Um(row.d50()); m.setD90Um(row.d90()); m.setMeanUm(row.mean());
            m.setMeasuredAt(row.measuredAt()); m.setStatus(MeasurementStatus.ACTIVE);
            int order = 0;
            BigDecimal cumulative = BigDecimal.ZERO;
            for (var p : row.points()) {
                SizeDistributionPoint dp = new SizeDistributionPoint();
                dp.setParticleMeasurement(m); dp.setBinSizeUm(p.binSizeUm()); dp.setVolumeFraction(p.volumeFraction());
                cumulative = cumulative.add(p.volumeFraction());
                dp.setCumulativeFraction(p.cumulativeFraction() == null ? cumulative : p.cumulativeFraction());
                dp.setDisplayOrder(order++);
                m.getDistribution().add(dp);
            }
            measurements.save(m);
            affectedBatches.add(batch.getId());
        }
    }

    private void rebuildComparisonsForSuccessfulJob(Long jobId) {
        Set<Long> batchIds = new HashSet<>();
        for (ImportFile f : files.findByJobIdOrderByIdAsc(jobId)) {
            if (f.getStatus() != ImportFileStatus.SUCCESS) continue;
            observations.findAll().stream().filter(o -> Objects.equals(o.getImportFileId(), f.getId()))
                    .forEach(o -> batchIds.add(o.getBatchId()));
            measurements.findAll().stream().filter(m -> Objects.equals(m.getImportFileId(), f.getId()))
                    .forEach(m -> samples.findById(m.getSampleId()).ifPresent(x -> batchIds.add(x.getBatchId())));
        }
        batchIds.forEach(id -> comparisonRules.evaluateBatch(id, true));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append("%02x".formatted(b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return String.valueOf(t.getMessage());
    }
}
