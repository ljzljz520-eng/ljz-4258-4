package com.dairy.homogenization.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class Requests {
    private Requests() {}
    public record MappingItemInput(@NotBlank String rawLabel, @NotBlank String calibratedStageCode,
                                   BigDecimal nominalPressureBar, Integer displayOrder) {}
    public record CorrectMappingRequest(@NotNull Long valveGroupId,
                                        @NotEmpty List<MappingItemInput> items,
                                        @NotBlank String changeSummary) {}
    public record CreateBatchRequest(@NotBlank String batchNumber, @NotBlank String productCode,
                                     @NotBlank String productName, @NotBlank String layerName,
                                     @NotNull java.time.OffsetDateTime startedAt,
                                     java.time.OffsetDateTime endedAt) {}
    public record CreateSamplePointRequest(@NotBlank String code, @NotBlank String name,
                                           @NotNull Long valveGroupId, @NotBlank String samplingLine,
                                           Integer productFlowOrder) {}
    public record CreateStableWindowRequest(@NotNull Long batchId, @NotNull Long valveGroupId,
                                            @NotBlank String stageCode,
                                            @NotNull java.time.OffsetDateTime startedAt,
                                            @NotNull java.time.OffsetDateTime endedAt,
                                            BigDecimal nominalPressureBar, String notes) {}
    public record AlgorithmVersionRequest(@NotBlank String instrumentCode, @NotBlank String measurementKind,
                                          @NotBlank String algorithmVersion,
                                          @NotNull java.time.OffsetDateTime effectiveFrom,
                                          java.time.OffsetDateTime effectiveTo, String description) {}
    public record ManualCandidateRequest(@NotNull Long beforeSampleId, @NotNull Long afterSampleId,
                                         @NotNull Long batchId, @NotNull Long valveGroupId, String stageCode) {}
    public record ReviewDecisionRequest(boolean approve, String decisionNote) {}
}
