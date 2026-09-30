package com.example.dairy.domain;
import jakarta.persistence.*; import java.util.UUID;
@Entity @Table(name="comparison_dependencies")
public class ComparisonDependency {
 @Id @Column(name="comparison_id") private UUID comparisonId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pressure_mapping_id") private PressureCalibrationMapping pressureMapping;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="particle_algorithm_before_id") private InstrumentAlgorithmVersion particleAlgorithmBefore;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="particle_algorithm_after_id") private InstrumentAlgorithmVersion particleAlgorithmAfter;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="comparison_id") private Comparison comparison;
 public UUID getComparisonId(){return comparisonId;} public void setComparisonId(UUID v){comparisonId=v;}
 public PressureCalibrationMapping getPressureMapping(){return pressureMapping;} public void setPressureMapping(PressureCalibrationMapping v){pressureMapping=v;}
 public InstrumentAlgorithmVersion getParticleAlgorithmBefore(){return particleAlgorithmBefore;} public void setParticleAlgorithmBefore(InstrumentAlgorithmVersion v){particleAlgorithmBefore=v;}
 public InstrumentAlgorithmVersion getParticleAlgorithmAfter(){return particleAlgorithmAfter;} public void setParticleAlgorithmAfter(InstrumentAlgorithmVersion v){particleAlgorithmAfter=v;}
 public Comparison getComparison(){return comparison;} public void setComparison(Comparison v){comparison=v;}
}
