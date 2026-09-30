package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="product_batches")
public class ProductBatch {
 @Id private UUID id;
 @Column(name="batch_no",nullable=false,unique=true) private String batchNo;
 @Column(name="product_name",nullable=false) private String productName;
 @Column(name="produced_at",nullable=false) private Instant producedAt;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public String getBatchNo(){return batchNo;} public void setBatchNo(String v){batchNo=v;}
 public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
 public Instant getProducedAt(){return producedAt;} public void setProducedAt(Instant v){producedAt=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
