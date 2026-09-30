package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.FindingSeverity;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComparabilityEngineTest {
    private final OffsetDateTime t = OffsetDateTime.parse("2026-09-30T09:10:00Z");
    private ComparabilityEngine.Side side(String sample, String layer, OffsetDateTime sampled,
                                         String stage, Long mapping, String algorithm) {
        return new ComparabilityEngine.Side(sample, "LINE-A-AFTER", "LINE-A", layer, sampled,
                stage, mapping, algorithm, true);
    }
    private ComparabilityEngine.Context context(ComparabilityEngine.Side b, ComparabilityEngine.Side a) {
        return new ComparabilityEngine.Context(b, a, true, true, Duration.ofMinutes(1440),
                true, "STAGE_180", true, List.of());
    }

    @Test
    void sampleLayerMismatchBlocksComparison() {
        var result = ComparabilityEngine.evaluate(context(
                side("B", "TOP", t.minusMinutes(20), "STAGE_180", 1L, "PSD-1.0"),
                side("A", "BULK", t, "STAGE_180", 1L, "PSD-1.0")));
        assertFalse(result.comparable());
        assertTrue(result.blockers().stream().anyMatch(f -> f.code().equals("LAYER_MISMATCH")));
    }

    @Test
    void lateBeforeSampleChronologyInversionBlocksComparison() {
        var result = ComparabilityEngine.evaluate(context(
                side("B", "BULK", t.plusMinutes(5), "STAGE_180", 1L, "PSD-1.0"),
                side("A", "BULK", t, "STAGE_180", 1L, "PSD-1.0")));
        assertFalse(result.comparable());
        assertTrue(result.blockers().stream().anyMatch(f -> f.code().equals("LATE_BEFORE_SAMPLE")));
    }

    @Test
    void particleAlgorithmChangeBlocksComparison() {
        var result = ComparabilityEngine.evaluate(context(
                side("B", "BULK", t.minusMinutes(20), "STAGE_180", 1L, "PSD-1.0"),
                side("A", "BULK", t, "STAGE_180", 1L, "PSD-2.0")));
        assertFalse(result.comparable());
        assertTrue(result.blockers().stream().anyMatch(f -> f.code().equals("PARTICLE_ALGORITHM_VERSION_MISMATCH")));
    }

    @Test
    void validEvidencePassesMandatoryChecks() {
        var result = ComparabilityEngine.evaluate(context(
                side("B", "BULK", t.minusMinutes(20), "STAGE_180", 1L, "PSD-1.0"),
                side("A", "BULK", t, "STAGE_180", 1L, "PSD-1.0")));
        assertTrue(result.comparable());
        assertEquals(FindingSeverity.PASS, result.findings().get(result.findings().size()-1).severity());
    }
}
