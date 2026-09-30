package com.dairy.homogenization.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "size_distribution_points")
public class SizeDistributionPoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "measurement_id", nullable = false)
    private ParticleMeasurement particleMeasurement;
    @Column(name = "bin_size_um", nullable = false, precision = 12, scale = 4) private BigDecimal binSizeUm;
    @Column(name = "volume_fraction", nullable = false, precision = 12, scale = 8) private BigDecimal volumeFraction;
    @Column(name = "cumulative_fraction", precision = 12, scale = 8) private BigDecimal cumulativeFraction;
    @Column(name = "display_order") private int displayOrder;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public ParticleMeasurement getParticleMeasurement() { return particleMeasurement; }
    public void setParticleMeasurement(ParticleMeasurement v) { this.particleMeasurement = v; }
    public BigDecimal getBinSizeUm() { return binSizeUm; } public void setBinSizeUm(BigDecimal v) { this.binSizeUm = v; }
    public BigDecimal getVolumeFraction() { return volumeFraction; } public void setVolumeFraction(BigDecimal v) { this.volumeFraction = v; }
    public BigDecimal getCumulativeFraction() { return cumulativeFraction; } public void setCumulativeFraction(BigDecimal v) { this.cumulativeFraction = v; }
    public int getDisplayOrder() { return displayOrder; } public void setDisplayOrder(int v) { this.displayOrder = v; }
}
