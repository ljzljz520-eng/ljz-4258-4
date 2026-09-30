package com.example.dairy.service;

import com.example.dairy.domain.Enums;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class InstrumentCsvParser {
    public ParsedInstrumentFile parse(byte[] content, String filename) {
        String text = new String(content);
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            String sampleHeader = requireLine(br);
            Map<String,String> sample = columns(sampleHeader);
            require(sample, "sampleCode", "measurementPointCode", "algorithmInstrumentCode", "algorithmName",
                    "algorithmVersion", "sampleType", "layer", "sampledAt");
            String pressureHeader = requireLine(br);
            Map<String,String> pressure = columns(pressureHeader);
            require(pressure, "stageOrder", "rawLabel", "rawBar", "measuredAt");
            String distributionHeader = requireLine(br);
            Map<String,String> dist = columns(distributionHeader);
            require(dist, "binUm", "volumePct");
            ParsedSample s = new ParsedSample(sample.get("sampleCode"), sample.get("measurementPointCode"),
                    sample.get("algorithmInstrumentCode"), sample.get("algorithmName"), sample.get("algorithmVersion"),
                    Enums.SampleType.valueOf(sample.get("sampleType")), Enums.Layer.valueOf(sample.get("layer")),
                    Instant.parse(sample.get("sampledAt")), new ArrayList<>(), new ArrayList<>());
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                Map<String,String> row = columns(line);
                if (row.containsKey("stageOrder") && !row.get("stageOrder").isBlank()) {
                    s.pressure().add(new ParsedPressure(Integer.parseInt(row.get("stageOrder")), row.get("rawLabel"),
                            new BigDecimal(row.get("rawBar")), Instant.parse(row.get("measuredAt"))));
                } else if (row.containsKey("binUm") && !row.get("binUm").isBlank()) {
                    s.distribution().add(new ParsedBin(new BigDecimal(row.get("binUm")), new BigDecimal(row.get("volumePct"))));
                }
            }
            if (s.pressure().isEmpty()) throw new ValidationException(filename + " 无压力阶段");
            if (s.distribution().isEmpty()) throw new ValidationException(filename + " 无粒径分布");
            s.pressure().sort(Comparator.comparingInt(ParsedPressure::stageOrder));
            s.distribution().sort(Comparator.comparing(ParsedBin::binUm));
            return new ParsedInstrumentFile(s);
        } catch (IOException e) {
            throw new ValidationException(filename + " 无法读取: " + e.getMessage());
        }
    }

    private String requireLine(BufferedReader br) throws IOException {
        String line = br.readLine();
        if (line == null || line.isBlank()) throw new ValidationException("仪器文件缺少必要段落");
        return line;
    }
    private Map<String,String> columns(String line) {
        String[] parts = line.split(",", -1);
        Map<String,String> map = new HashMap<>();
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }
    private void require(Map<String,String> row, String... keys) {
        for (String key : keys) if (!row.containsKey(key) || row.get(key).isBlank())
            throw new ValidationException("缺少字段 " + key);
    }

    public record ParsedInstrumentFile(ParsedSample sample) {}
    public record ParsedSample(String sampleCode, String measurementPointCode, String instrumentCode,
                               String algorithmName, String algorithmVersion, Enums.SampleType type, Enums.Layer layer,
                               Instant sampledAt, List<ParsedPressure> pressure, List<ParsedBin> distribution) {}
    public record ParsedPressure(int stageOrder, String rawLabel, BigDecimal rawBar, Instant measuredAt) {}
    public record ParsedBin(BigDecimal binUm, BigDecimal volumePct) {}
}
