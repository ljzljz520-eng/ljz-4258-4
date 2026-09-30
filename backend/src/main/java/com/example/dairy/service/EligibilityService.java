package com.example.dairy.service;

import com.example.dairy.domain.*;
import com.example.dairy.dto.Dtos.ReasonResponse;
import com.example.dairy.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EligibilityService {
    private final ImportJobRepository importJobs;
    private final PressureCalibrationMappingRepository mappings;

    public EligibilityService(ImportJobRepository importJobs, PressureCalibrationMappingRepository mappings) {
        this.importJobs = importJobs;
        this.mappings = mappings;
    }

    public EligibilityResult evaluate(BatchSegment segment, Sample before, Sample after) {
        List<ReasonResponse> reasons = new ArrayList<>();
        ReasonResponse importReason = importReason(checkImports(segment));
        if (importReason != null) reasons.add(importReason);
        if (!before.getSamplingLine().getId().equals(after.getSamplingLine().getId())
                || !before.getSamplingLine().getId().equals(segment.getSamplingLine().getId())) {
            reasons.add(block("SAMPLING_LINE_MISMATCH", "前后样必须来自批段指定的同一取样管线。"));
        }
        if (!before.getProductBatch().getId().equals(after.getProductBatch().getId())
                || !before.getProductBatch().getId().equals(segment.getProductBatch().getId())) {
            reasons.add(block("PRODUCT_BATCH_MISMATCH", "前后样必须属于同一产品批。"));
        }
        if (before.getLayer() != after.getLayer()) {
            reasons.add(block("LAYER_MISMATCH", "样品分层不同：前样=%s，后样=%s。".formatted(before.getLayer(), after.getLayer())));
        }
        if (before.getSampleType() != Enums.SampleType.BEFORE || after.getSampleType() != Enums.SampleType.AFTER) {
            reasons.add(block("SAMPLE_DIRECTION_INVALID", "比较方向必须是前样到后样。"));
        }
        if (!windowContains(segment.getBaselineStart(), segment.getBaselineEnd(), before.getSampledAt())) {
            reasons.add(block("BEFORE_OUTSIDE_BASELINE_WINDOW", "前样实际取样时间不在基线窗口内。"));
        }
        if (!windowContains(segment.getStableStart(), segment.getStableEnd(), after.getSampledAt())) {
            reasons.add(block("AFTER_OUTSIDE_STABLE_WINDOW", "后样实际取样时间不在稳定窗口内。"));
        }
        if (!before.getSampledAt().isBefore(after.getSampledAt())) {
            reasons.add(block("BEFORE_NOT_EARLIER_THAN_AFTER", "按实际取样时间，前样不早于后样；到达先后不能替代取样先后。"));
        }
        if (!before.getAlgorithmVersion().getId().equals(after.getAlgorithmVersion().getId())) {
            reasons.add(block("PARTICLE_ALGORITHM_MISMATCH",
                    "粒度算法版本不同：%s %s vs %s %s。".formatted(
                            before.getAlgorithmVersion().getAlgorithmName(), before.getAlgorithmVersion().getVersion(),
                            after.getAlgorithmVersion().getAlgorithmName(), after.getAlgorithmVersion().getVersion())));
        }
        PressureCalibrationMapping commonMapping = checkPressureStages(segment, before, after, reasons);
        return new EligibilityResult(List.copyOf(reasons), commonMapping);
    }

    private String checkImports(BatchSegment segment) {
        List<ImportJob> jobs = importJobs.findBySegmentIdOrderByCreatedAtAsc(segment.getId());
        if (jobs.isEmpty()) return blockCode("IMPORT_NOT_COMPLETE", "还没有导入任务。");
        ImportJob latest = jobs.get(jobs.size() - 1);
        return switch (latest.getStatus()) {
            case SUCCEEDED -> null;
            case RUNNING, PENDING -> blockCode("IMPORT_NOT_COMPLETE", "最新导入任务仍在处理，比较证据可能尚未完整。");
            case COMPLETED_WITH_FAILURES -> blockCode("IMPORT_PARTIAL_FAILURE", "最新导入任务仅部分文件成功，不能留下完整假象。");
            case FAILED -> blockCode("IMPORT_FAILED", "最新导入任务失败；请修正文件后重试。");
            case CANCELLED -> blockCode("IMPORT_CANCELLED", "最新导入任务已取消。");
        };
    }

    private PressureCalibrationMapping checkPressureStages(BatchSegment segment, Sample before, Sample after, List<ReasonResponse> reasons) {
        List<PressureReading> bs = before.getPressureReadings().stream().sorted(Comparator.comparingInt(PressureReading::getStageOrder)).toList();
        List<PressureReading> as = after.getPressureReadings().stream().sorted(Comparator.comparingInt(PressureReading::getStageOrder)).toList();
        if (bs.isEmpty() || as.isEmpty()) {
            reasons.add(block("PRESSURE_STAGE_MISSING", "至少一侧缺少压力阶段。"));
            return null;
        }
        if (bs.size() != as.size()) {
            reasons.add(block("PRESSURE_STAGE_COUNT_MISMATCH", "压力阶段数量不同：%d vs %d。".formatted(bs.size(), as.size())));
        }
        Set<UUID> mappingIds = new HashSet<>();
        for (int i = 0; i < Math.min(bs.size(), as.size()); i++) {
            PressureReading b = bs.get(i), a = as.get(i);
            if (b.getMapping() == null || a.getMapping() == null) {
                reasons.add(block("PRESSURE_MAPPING_MISSING", "阶段 %d 的仪器原始标签尚无校准映射。".formatted(i + 1)));
                continue;
            }
            if (!b.getMapping().getId().equals(a.getMapping().getId())) {
                reasons.add(block("PRESSURE_MAPPING_MISMATCH", "阶段 %d 使用不同校准映射。".formatted(i + 1)));
            }
            if (!b.getMapping().getCanonicalStageLabel().equals(a.getMapping().getCanonicalStageLabel())) {
                reasons.add(block("PRESSURE_STAGE_MISMATCH", "阶段 %d 规范标签不同。".formatted(i + 1)));
            }
            mappingIds.add(b.getMapping().getId());
            mappingIds.add(a.getMapping().getId());
        }
        if (mappingIds.size() > 1) {
            reasons.add(block("PRESSURE_MAPPING_INCONSISTENT", "同一比较包含多个压力映射版本，不能解释为同一次互换。"));
        }
        if (mappingIds.size() != 1) return null;
        UUID mappingId = mappingIds.iterator().next();
        PressureCalibrationMapping mapping = mappings.findById(mappingId).orElse(null);
        if (mapping != null && !mapping.isActive()) {
            reasons.add(block("PRESSURE_MAPPING_CHANGED", "校准修正级别已变更，历史依赖比较已过期；刷新并重新确认后才能审核。"));
        }
        return mapping;
    }

    private boolean windowContains(java.time.Instant start, java.time.Instant end, java.time.Instant value) {
        return !value.isBefore(start) && !value.isAfter(end);
    }
    private ReasonResponse importReason(String encoded) {
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|",2);
        return new ReasonResponse(parts[0], parts[1], "BLOCKER");
    }
    private ReasonResponse block(String code, String detail) { return new ReasonResponse(code, detail, "BLOCKER"); }
    private String blockCode(String code, String detail) { return code + "|" + detail; }
}
