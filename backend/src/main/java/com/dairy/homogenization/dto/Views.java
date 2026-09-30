package com.dairy.homogenization.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class Views {
    private Views() {}
    public record IdName(Long id, String name) {}
    public record FindingView(String code, String severity, String message, String detail) {}
    public record DistributionPoint(BigDecimal binSizeUm, BigDecimal volumeFraction, BigDecimal cumulativeFraction) {}
    public record CurveView(String label, String sampleCode, String algorithmVersion,
                            String rawPressureLabel, String calibratedStageCode,
                            BigDecimal d10Um, BigDecimal d50Um, BigDecimal d90Um, BigDecimal meanUm,
                            OffsetDateTime sampledAt, List<DistributionPoint> distribution) {}
    public record ComparisonView(Long id, Long beforeSampleId, Long afterSampleId, Long batchId, Long valveGroupId,
                                 String stageCode, String status, String origin, boolean comparable,
                                 String reasonSummary, String fingerprint, List<FindingView> findings,
                                 CurveView before, CurveView after, String reviewStatus, String reviewedBy) {}
    public record ImportFileView(Long id, String fileName, String fileType, String status,
                                 int attempts, String errorMessage) {}
    public record ImportJobView(Long id, String externalJobId, String status, String uploadedBy,
                                int totalFiles, int succeededFiles, int failedFiles, String errorSummary,
                                List<ImportFileView> files) {}
    public record SampleView(Long id, String sampleCode, Long batchId, String batchNumber, Long samplePointId,
                             String samplePointCode, String samplingLine, Long valveGroupId, String position,
                             String layerName, OffsetDateTime sampledAt, OffsetDateTime receivedAt,
                             Integer transportDelaySeconds) {}
    public record PressurePointView(OffsetDateTime observedAt, String rawLabel, String calibratedStageCode,
                                    Long mappingVersionId, BigDecimal observedPressureBar) {}
    public record MappingView(Long id, Long valveGroupId, Integer version, String status,
                              OffsetDateTime effectiveFrom, String changeSummary,
                              List<MappingItemView> items) {}
    public record MappingItemView(String rawLabel, String calibratedStageCode,
                                  BigDecimal nominalPressureBar, Integer displayOrder) {}
    public record ReviewView(Long id, Long comparisonId, String status, String requestedBy, String reviewedBy,
                             OffsetDateTime requestedAt, OffsetDateTime reviewedAt, String decisionNote,
                             String fingerprint) {}
}
