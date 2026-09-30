package com.example.dairy.service;

import com.example.dairy.domain.*;
import com.example.dairy.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class FileImportProcessor {
    private final ImportFileRepository files;
    private final SampleRepository samples;
    private final MeasurementPointRepository points;
    private final AlgorithmVersionRepository algorithms;
    private final PressureCalibrationMappingRepository mappings;
    private final InstrumentCsvParser parser = new InstrumentCsvParser();

    public FileImportProcessor(ImportFileRepository files, SampleRepository samples, MeasurementPointRepository points,
                               AlgorithmVersionRepository algorithms, PressureCalibrationMappingRepository mappings) {
        this.files = files; this.samples = samples; this.points = points;
        this.algorithms = algorithms; this.mappings = mappings;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processFile(UUID fileId) {
        ImportFile file = files.findById(fileId).orElseThrow(() -> new NotFoundException("导入文件不存在"));
        ImportJob job = file.getJob();
        BatchSegment segment = job.getSegment();
        if (segment.getStatus() != Enums.SegmentStatus.OPEN)
            throw new ConflictException("批段已锁定或签发，文件不能写入。");
        InstrumentCsvParser.ParsedInstrumentFile parsed = parser.parse(file.getContent(), file.getFilename());
        InstrumentCsvParser.ParsedSample row = parsed.sample();
        if (samples.findBySampleCode(row.sampleCode()).isPresent())
            throw new ValidationException("样品编号已存在: " + row.sampleCode());

        MeasurementPoint point = points.findByCode(row.measurementPointCode())
                .orElseThrow(() -> new ValidationException("未知测点: " + row.measurementPointCode()));
        InstrumentAlgorithmVersion algorithm = algorithms
                .findFirstByInstrumentCodeAndAlgorithmNameAndVersionAndActiveTrue(row.instrumentCode(), row.algorithmName(), row.algorithmVersion())
                .orElseThrow(() -> new ValidationException("未知或停用的粒度算法版本: %s %s %s"
                        .formatted(row.instrumentCode(), row.algorithmName(), row.algorithmVersion())));

        Sample sample = new Sample();
        sample.setId(UUID.randomUUID());
        sample.setSegment(segment);
        sample.setSamplingLine(segment.getSamplingLine());
        sample.setProductBatch(segment.getProductBatch());
        sample.setMeasurementPoint(point);
        sample.setAlgorithmVersion(algorithm);
        sample.setImportFile(file);
        sample.setSampleCode(row.sampleCode());
        sample.setSampleType(row.type());
        sample.setLayer(row.layer());
        sample.setSampledAt(row.sampledAt());
        sample.setReceivedAt(Instant.now());
        for (InstrumentCsvParser.ParsedPressure p : row.pressure()) {
            PressureReading reading = new PressureReading();
            reading.setSample(sample);
            reading.setStageOrder(p.stageOrder());
            reading.setRawLabel(p.rawLabel());
            reading.setRawValueBar(p.rawBar());
            reading.setMeasuredAt(p.measuredAt());
            PressureCalibrationMapping mapping = mappings
                    .findFirstByValveGroup_IdAndMeasurementPoint_IdAndInstrumentLabelAndActiveOrderByVersionDesc(
                            segment.getValveGroup().getId(), point.getId(), p.rawLabel(), true).orElse(null);
            if (mapping != null) {
                reading.setMapping(mapping);
                reading.setCorrectedValueBar(p.rawBar().multiply(mapping.getSlope()).add(mapping.getIntercept()));
            }
            sample.getPressureReadings().add(reading);
        }
        for (InstrumentCsvParser.ParsedBin b : row.distribution()) {
            ParticleDistribution d = new ParticleDistribution();
            d.setSample(sample);
            d.setBinUm(b.binUm());
            d.setVolumePct(b.volumePct());
            sample.getParticleDistributions().add(d);
        }
        try {
            samples.saveAndFlush(sample);
        } catch (DataIntegrityViolationException e) {
            throw new ValidationException("文件数据违反唯一或检查约束，整个文件回滚。", e);
        }
    }
}
