package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="instrument_algorithm_versions")
public class InstrumentAlgorithmVersion {
 @Id private UUID id;
 @Column(name="instrument_code",nullable=false) private String instrumentCode;
 @Column(name="algorithm_name",nullable=false) private String algorithmName;
 @Column(nullable=false) private String version;
 private String checksum;
 private boolean active=true;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public String getInstrumentCode(){return instrumentCode;} public void setInstrumentCode(String v){instrumentCode=v;}
 public String getAlgorithmName(){return algorithmName;} public void setAlgorithmName(String v){algorithmName=v;}
 public String getVersion(){return version;} public void setVersion(String v){version=v;}
 public String getChecksum(){return checksum;} public void setChecksum(String v){checksum=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
