package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "product_batches")
public class ProductBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_number", nullable = false, unique = true) private String batchNumber;
    @Column(name = "product_code", nullable = false) private String productCode;
    @Column(name = "product_name", nullable = false) private String productName;
    @Column(name = "layer_name", nullable = false) private String layerName = "BULK";
    @Column(name = "started_at", nullable = false) private OffsetDateTime startedAt;
    @Column(name = "ended_at") private OffsetDateTime endedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BatchStatus status = BatchStatus.OPEN;
    @Column(name = "created_at", insertable = false, updatable = false) private OffsetDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getBatchNumber() { return batchNumber; } public void setBatchNumber(String v) { this.batchNumber = v; }
    public String getProductCode() { return productCode; } public void setProductCode(String v) { this.productCode = v; }
    public String getProductName() { return productName; } public void setProductName(String v) { this.productName = v; }
    public String getLayerName() { return layerName; } public void setLayerName(String v) { this.layerName = v; }
    public OffsetDateTime getStartedAt() { return startedAt; } public void setStartedAt(OffsetDateTime v) { this.startedAt = v; }
    public OffsetDateTime getEndedAt() { return endedAt; } public void setEndedAt(OffsetDateTime v) { this.endedAt = v; }
    public BatchStatus getStatus() { return status; } public void setStatus(BatchStatus v) { this.status = v; }
}
