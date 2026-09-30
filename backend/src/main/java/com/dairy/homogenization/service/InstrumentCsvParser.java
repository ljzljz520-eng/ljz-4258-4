package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.SamplePosition;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Component
public class InstrumentCsvParser {
    public List<CsvParseModels.PressureRow> parsePressure(String content) {
        List<Map<String,String>> rows = parse(content);
        List<CsvParseModels.PressureRow> result = new ArrayList<>();
        for (Map<String,String> r : rows) {
            result.add(new CsvParseModels.PressureRow(
                    required(r,"batch_number"), required(r,"valve_group"), required(r,"raw_label"),
                    new BigDecimal(required(r,"pressure_bar")),
                    OffsetDateTime.parse(required(r,"observed_at"))));
        }
        return result;
    }

    public List<CsvParseModels.ParticleRow> parseParticle(String content) {
        List<Map<String,String>> rows = parse(content);
        Map<String, List<Map<String,String>>> bySample = new LinkedHashMap<>();
        for (Map<String,String> row : rows) bySample.computeIfAbsent(required(row,"sample_code"), k -> new ArrayList<>()).add(row);
        List<CsvParseModels.ParticleRow> parsed = new ArrayList<>();
        for (var entry : bySample.entrySet()) {
            Map<String,String> first = entry.getValue().get(0);
            SamplePosition.valueOf(required(first, "position"));
            List<CsvParseModels.SizePoint> points = new ArrayList<>();
            int order = 0;
            for (Map<String,String> r : entry.getValue()) {
                points.add(new CsvParseModels.SizePoint(
                        new BigDecimal(required(r,"bin_size_um")),
                        new BigDecimal(required(r,"volume_fraction")),
                        optionalDecimal(r,"cumulative_fraction")));
                order++;
            }
            parsed.add(new CsvParseModels.ParticleRow(
                    required(first,"batch_number"), required(first,"sample_code"),
                    required(first,"sample_point"), required(first,"position"), required(first,"layer"),
                    OffsetDateTime.parse(required(first,"sampled_at")),
                    parseTime(first.get("received_at")), required(first,"instrument"),
                    required(first,"algorithm_version"),
                    OffsetDateTime.parse(required(first,"measured_at")),
                    optionalDecimal(first,"d10_um"), optionalDecimal(first,"d50_um"),
                    optionalDecimal(first,"d90_um"), optionalDecimal(first,"mean_um"), points));
        }
        return parsed;
    }

    private List<Map<String,String>> parse(String content) {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) throw new IllegalArgumentException("CSV 缺少表头");
            List<String> headers = parseLine(headerLine).stream().map(String::trim).toList();
            List<Map<String,String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> values = parseLine(line);
                if (values.size() != headers.size())
                    throw new IllegalArgumentException("CSV 列数不匹配: " + line);
                Map<String,String> row = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) row.put(headers.get(i), values.get(i).trim());
                rows.add(row);
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("CSV 没有数据行");
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                values.add(current.toString()); current.setLength(0);
            } else current.append(c);
        }
        values.add(current.toString());
        return values;
    }

    private String required(Map<String,String> row, String key) {
        String v = row.get(key);
        if (v == null) throw new IllegalArgumentException("缺少必填列: " + key);
        v = v.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("必填列为空: " + key);
        return v;
    }
    private BigDecimal optionalDecimal(Map<String,String> row, String key) {
        String v = row.get(key);
        return v == null || v.isBlank() ? null : new BigDecimal(v.trim());
    }
    private OffsetDateTime parseTime(String v) {
        return v == null || v.isBlank() ? null : OffsetDateTime.parse(v.trim());
    }
}
