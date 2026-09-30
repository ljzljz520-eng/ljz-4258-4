package com.example.dairy.domain;
import jakarta.persistence.*; import java.math.BigDecimal; import java.util.UUID;
@Entity @Table(name="particle_distributions")
public class ParticleDistribution {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sample_id") private Sample sample;
 @Column(name="bin_um",nullable=false,precision=12,scale=4) private BigDecimal binUm;
 @Column(name="volume_pct",nullable=false,precision=9,scale=6) private BigDecimal volumePct;
 @PrePersist void assign(){ if(id==null) id=UUID.randomUUID(); }
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public Sample getSample(){return sample;} public void setSample(Sample v){sample=v;}
 public BigDecimal getBinUm(){return binUm;} public void setBinUm(BigDecimal v){binUm=v;}
 public BigDecimal getVolumePct(){return volumePct;} public void setVolumePct(BigDecimal v){volumePct=v;}
}
