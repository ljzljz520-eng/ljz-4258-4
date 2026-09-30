package com.dairy.homogenization.web;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.dto.Views.*;
import com.dairy.homogenization.repository.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ViewAssembler {
    private final SampleRepository samples;
    private final SamplePointRepository points;
    private final ProductBatchRepository batches;
    private final ParticleMeasurementRepository measurements;
    private final InstrumentAlgorithmVersionRepository algorithms;
    private final PressureObservationRepository pressure;
    private final ComparabilityFindingRepository findings;
    private final ReviewRepository reviews;
    private final ImportFileRepository files;
    private final ComparisonCandidateRepository comparisonRepository;
    public ViewAssembler(SampleRepository samples, SamplePointRepository points, ProductBatchRepository batches,
                         ParticleMeasurementRepository measurements,
                         InstrumentAlgorithmVersionRepository algorithms, PressureObservationRepository pressure,
                         ComparabilityFindingRepository findings, ReviewRepository reviews,
                         ImportFileRepository files, ComparisonCandidateRepository comparisonRepository) {
        this.samples = samples; this.points = points; this.batches = batches;
        this.measurements = measurements; this.algorithms = algorithms; this.pressure = pressure;
        this.findings = findings; this.reviews = reviews; this.files = files;
        this.comparisonRepository = comparisonRepository;
    }

    public ComparisonView comparison(ComparisonCandidate c) {
        CurveView before = curve(c.getBeforeSampleId());
        CurveView after = curve(c.getAfterSampleId());
        List<FindingView> fs = findings.findByComparisonIdOrderBySeverityDescIdAsc(c.getId()).stream()
                .map(f -> new FindingView(f.getCode(), f.getSeverity().name(), f.getMessage(), f.getDetail())).toList();
        String reviewStatus = reviews.findByComparisonId(c.getId()).map(r -> r.getStatus().name()).orElse(null);
        String reviewedBy = reviews.findByComparisonId(c.getId()).map(Review::getReviewedBy).orElse(null);
        return new ComparisonView(c.getId(), c.getBeforeSampleId(), c.getAfterSampleId(), c.getBatchId(),
                c.getValveGroupId(), c.getStageCode(), c.getStatus().name(), c.getOrigin().name(), c.isComparable(),
                c.getReasonSummary(), c.getFingerprint(), fs, before, after, reviewStatus, reviewedBy);
    }

    public CurveView curve(Long sampleId) {
        Sample s = samples.findById(sampleId).orElse(null);
        if (s == null) return null;
        SamplePoint point = points.findById(s.getSamplePointId()).orElseThrow();
        ParticleMeasurement m = measurements.findBySampleIdAndStatus(sampleId, MeasurementStatus.ACTIVE).orElse(null);
        String alg = null;
        List<DistributionPoint> dist = List.of();
        if (m != null) {
            alg = algorithms.findById(m.getAlgorithmVersionId()).map(InstrumentAlgorithmVersion::getAlgorithmVersion).orElse(null);
            dist = m.getDistribution().stream()
                    .map(d -> new DistributionPoint(d.getBinSizeUm(), d.getVolumeFraction(), d.getCumulativeFraction())).toList();
        }
        PressureObservation nearest = pressure.findByBatchIdAndValveGroupIdOrderByObservedAtAsc(s.getBatchId(), s.getValveGroupId()).stream()
                .min(Comparator.comparingLong(o -> Math.abs(java.time.Duration.between(o.getObservedAt(), s.getSampledAt()).toSeconds())))
                .orElse(null);
        return new CurveView(point.getName(), s.getSampleCode(), alg,
                nearest == null ? null : nearest.getRawLabel(),
                nearest == null ? null : nearest.getCalibratedStageCode(),
                m == null ? null : m.getD10Um(), m == null ? null : m.getD50Um(),
                m == null ? null : m.getD90Um(), m == null ? null : m.getMeanUm(),
                s.getSampledAt(), dist);
    }

    public SampleView sample(Sample s) {
        SamplePoint p = points.findById(s.getSamplePointId()).orElseThrow();
        String batch = batches.findById(s.getBatchId()).map(ProductBatch::getBatchNumber).orElse(null);
        return new SampleView(s.getId(), s.getSampleCode(), s.getBatchId(), batch, s.getSamplePointId(),
                p.getCode(), p.getSamplingLine(), s.getValveGroupId(), s.getPosition().name(), s.getLayerName(),
                s.getSampledAt(), s.getReceivedAt(), s.getTransportDelaySeconds());
    }

    public ImportJobView job(ImportJob job) {
        return new ImportJobView(job.getId(), job.getExternalJobId(), job.getStatus().name(), job.getUploadedBy(),
                job.getTotalFiles(), job.getSucceededFiles(), job.getFailedFiles(), job.getErrorSummary(),
                files.findByJobIdOrderByIdAsc(job.getId()).stream().map(f -> new ImportFileView(
                        f.getId(), f.getFileName(), f.getFileType().name(), f.getStatus().name(),
                        f.getAttempts(), f.getErrorMessage())).toList());
    }

    public MappingView mapping(PressureMappingVersion m) {
        return new MappingView(m.getId(), m.getValveGroupId(), m.getVersionNumber(), m.getStatus().name(),
                m.getEffectiveFrom(), m.getChangeSummary(), m.getItems().stream()
                .map(i -> new MappingItemView(i.getRawLabel(), i.getCalibratedStageCode(),
                        i.getNominalPressureBar(), i.getDisplayOrder())).toList());
    }

    public ReviewView review(Review r) {
        String fp = comparisonsByReview(r);
        return new ReviewView(r.getId(), r.getComparisonId(), r.getStatus().name(), r.getRequestedBy(),
                r.getReviewedBy(), r.getRequestedAt(), r.getReviewedAt(), r.getDecisionNote(), fp);
    }
    private String comparisonsByReview(Review r) {
        return comparisonRepository.findById(r.getComparisonId()).map(ComparisonCandidate::getFingerprint).orElse(null);
    }
}
