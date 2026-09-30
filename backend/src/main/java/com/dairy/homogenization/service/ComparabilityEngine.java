package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.FindingSeverity;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Pure domain rule engine. It only states whether evidence can be compared; it never recommends
 * pressure settings and cannot control equipment.
 */
public final class ComparabilityEngine {
    private ComparabilityEngine() {}

    public record Finding(String code, FindingSeverity severity, String message, String detail) {}
    public record Side(String sampleCode, String samplePointCode, String samplingLine, String layer,
                       OffsetDateTime sampledAt, String stageCode, Long pressureMappingVersionId,
                       String algorithmVersion, boolean hasMeasurement) {}
    public record Context(Side before, Side after, boolean sameBatch, boolean sameValveGroup,
                          Duration maxTransportDelay, boolean afterInStableWindow, String stableWindowStage,
                          boolean evidenceComplete, List<String> evidenceWarnings) {}

    public record Result(boolean comparable, List<Finding> findings) {
        public List<Finding> blockers() {
            return findings.stream().filter(f -> f.severity == FindingSeverity.BLOCKER).toList();
        }
        public String summary() {
            List<String> blockers = blockers().stream().map(Finding::message).toList();
            return blockers.isEmpty() ? "可比：所有强制检查通过" : String.join("；", blockers);
        }
    }

    public static Result evaluate(Context context) {
        List<Finding> findings = new ArrayList<>();
        Side before = context.before();
        Side after = context.after();

        check(!context.sameBatch(), "BATCH_MISMATCH", "产品批不一致",
                "只能比较同一产品批的前样/后样。", findings);
        check(!context.sameValveGroup(), "VALVE_GROUP_MISMATCH", "均质阀组不一致",
                "不同阀组的剪切路径与校准证据不能直接比较。", findings);
        check(!safeEquals(before.samplingLine(), after.samplingLine()), "SAMPLING_LINE_MISMATCH",
                "取样管线不一致", "前样与后样必须来自同一条取样管线。", findings);
        check(!safeEquals(before.layer(), after.layer()), "LAYER_MISMATCH",
                "样品分层不一致", "表层/中层/底层等分层必须相同；不允许跨层比较。", findings);

        boolean chronologyOk = after.sampledAt().isAfter(before.sampledAt());
        check(!chronologyOk, "LATE_BEFORE_SAMPLE", "迟到前样",
                "前样采样时间晚于后样，存在运输/录入倒挂，不能按时间顺序比较。", findings);
        if (chronologyOk) {
            long delay = Duration.between(before.sampledAt(), after.sampledAt()).getSeconds();
            check(delay > context.maxTransportDelay().getSeconds(), "TRANSPORT_DELAY_EXCEEDED",
                    "运输时延超出窗口", "前后样相隔 " + delay + " 秒，超过允许值 "
                            + context.maxTransportDelay().getSeconds() + " 秒。", findings);
        }

        check(!context.afterInStableWindow(), "STABLE_WINDOW_MISS",
                "后样未落在稳定窗口", "比较点必须落入该压力阶段的稳定窗口（含配置容差）。", findings);
        if (context.afterInStableWindow() && after.stageCode() != null
                && !Objects.equals(after.stageCode(), context.stableWindowStage())) {
            findings.add(new Finding("STAGE_WINDOW_MISMATCH", FindingSeverity.BLOCKER,
                    "压力阶段与稳定窗口不一致",
                    "仪器/校准阶段为 " + after.stageCode() + "，稳定窗口为 " + context.stableWindowStage()));
        }

        check(before.stageCode() == null || after.stageCode() == null, "PRESSURE_STAGE_UNRESOLVED",
                "压力阶段无法解析", "原始压力标签缺少有效校准映射，无法确定比较阶段。", findings);
        if (before.stageCode() != null && after.stageCode() != null
                && !safeEquals(before.stageCode(), after.stageCode())) {
            findings.add(new Finding("PRESSURE_STAGE_MISMATCH", FindingSeverity.BLOCKER,
                    "压力阶段不一致", "前样阶段=" + before.stageCode() + "，后样阶段=" + after.stageCode()));
        }
        if (before.pressureMappingVersionId() != null && after.pressureMappingVersionId() != null
                && !Objects.equals(before.pressureMappingVersionId(), after.pressureMappingVersionId())) {
            findings.add(new Finding("PRESSURE_MAPPING_VERSION_MISMATCH", FindingSeverity.BLOCKER,
                    "压力校准映射版本不一致",
                    "前样使用映射#" + before.pressureMappingVersionId() + "，后样使用映射#"
                            + after.pressureMappingVersionId()
                            + "；修正级别映射会使未锁定比较过期。"));
        }

        check(!before.hasMeasurement() || !after.hasMeasurement(), "MEASUREMENT_MISSING",
                "粒度测量不完整", "前样和后样都必须有有效粒径测量。", findings);
        if (before.hasMeasurement() && after.hasMeasurement()
                && !safeEquals(before.algorithmVersion(), after.algorithmVersion())) {
            findings.add(new Finding("PARTICLE_ALGORITHM_VERSION_MISMATCH", FindingSeverity.BLOCKER,
                    "粒度仪器算法版本不一致",
                    "前样=" + before.algorithmVersion() + "，后样=" + after.algorithmVersion()
                            + "；只能比较同一算法版本，换版后应重新测量或另建换版验证。"));
        }

        check(!context.evidenceComplete(), "EVIDENCE_IMPORT_INCOMPLETE",
                "导入证据未完整成功", "相关仪器文件所在导入任务仍在处理、失败或仅部分成功；审核前不得形成完整假象。", findings);
        for (String warning : context.evidenceWarnings()) {
            findings.add(new Finding("EVIDENCE_WARNING", FindingSeverity.WARNING, warning, null));
        }

        boolean comparable = findings.stream().noneMatch(f -> f.severity() == FindingSeverity.BLOCKER);
        Set<String> codes = new LinkedHashSet<>();
        for (Finding f : findings) codes.add(f.code());
        if (comparable && codes.stream().noneMatch(c -> c.equals("COMPARABLE"))) {
            findings.add(new Finding("COMPARABLE", FindingSeverity.PASS, "强制检查通过",
                    "取样管线、产品批、分层、时序、稳定窗口、压力映射与粒度算法均一致。"));
        }
        return new Result(comparable, List.copyOf(findings));
    }

    private static void check(boolean invalid, String code, String message, String detail, List<Finding> out) {
        if (invalid) out.add(new Finding(code, FindingSeverity.BLOCKER, message, detail));
    }
    private static boolean safeEquals(Object a, Object b) { return Objects.equals(a, b); }
}
