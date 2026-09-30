package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "comparability_findings")
public class ComparabilityFinding {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "comparison_id", nullable = false) private Long comparisonId;
    @Column(nullable = false) private String code;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FindingSeverity severity;
    @Column(nullable = false) private String message;
    private String detail;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getComparisonId() { return comparisonId; } public void setComparisonId(Long v) { this.comparisonId = v; }
    public String getCode() { return code; } public void setCode(String v) { this.code = v; }
    public FindingSeverity getSeverity() { return severity; } public void setSeverity(FindingSeverity v) { this.severity = v; }
    public String getMessage() { return message; } public void setMessage(String v) { this.message = v; }
    public String getDetail() { return detail; } public void setDetail(String v) { this.detail = v; }
}
