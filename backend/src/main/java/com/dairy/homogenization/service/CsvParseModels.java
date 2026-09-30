package com.dairy.homogenization.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public final class CsvParseModels {
    private CsvParseModels() {}
    public record PressureRow(String batchNumber, String valveGroupCode, String rawLabel,
                              BigDecimal pressureBar, OffsetDateTime observedAt) {}
    public record SizePoint(BigDecimal binSizeUm, BigDecimal volumeFraction, BigDecimal cumulativeFraction) {}
    public record ParticleRow(String batchNumber, String sampleCode, String samplePointCode, String position,
                              String layer, OffsetDateTime sampledAt, OffsetDateTime receivedAt,
                              String instrumentCode, String algorithmVersion, OffsetDateTime measuredAt,
                              BigDecimal d10, BigDecimal d50, BigDecimal d90, BigDecimal mean,
                              List<SizePoint> points) {}
}
