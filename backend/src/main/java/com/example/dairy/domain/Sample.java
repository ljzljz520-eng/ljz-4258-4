package com.example.dairy.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.ArrayList; import java.util.List; import java.util.UUID;
@Entity @Table(name="samples")
public class Sample {
 @Id private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="segment_id") private BatchSegment segment;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sampling_line_id") private SamplingLine samplingLine;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_batch_id") private ProductBatch productBatch;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="measurement_point_id") private MeasurementPoint measurementPoint;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="algorithm_version_id") private InstrumentAlgorithmVersion algorithmVersion;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="import_file_id") private ImportFile importFile;
 @Column(name="sample_code",nullable=false,unique=true) private String sampleCode;
 @Enumerated(EnumType.STRING) @Column(name="sample_type",nullable=false,length=8) private Enums.SampleType sampleType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=8) private Enums.Layer layer;
 @Column(name="sampled_at",nullable=false) private Instant sampledAt;
 @Column(name="received_at",nullable=false) private Instant receivedAt;
 @Column(name="created_at") private Instant createdAt=Instant.now();
 @OneToMany(mappedBy="sample",cascade=CascadeType.ALL,orphanRemoval=true) private List<PressureReading> pressureReadings=new ArrayList<>();
 @OneToMany(mappedBy="sample",cascade=CascadeType.ALL,orphanRemoval=true) private List<ParticleDistribution> particleDistributions=new ArrayList<>();
 public UUID getId(){return id;} public void setId(UUID v){id=v;}
 public BatchSegment getSegment(){return segment;} public void setSegment(BatchSegment v){segment=v;}
 public SamplingLine getSamplingLine(){return samplingLine;} public void setSamplingLine(SamplingLine v){samplingLine=v;}
 public ProductBatch getProductBatch(){return productBatch;} public void setProductBatch(ProductBatch v){productBatch=v;}
 public MeasurementPoint getMeasurementPoint(){return measurementPoint;} public void setMeasurementPoint(MeasurementPoint v){measurementPoint=v;}
 public InstrumentAlgorithmVersion getAlgorithmVersion(){return algorithmVersion;} public void setAlgorithmVersion(InstrumentAlgorithmVersion v){algorithmVersion=v;}
 public ImportFile getImportFile(){return importFile;} public void setImportFile(ImportFile v){importFile=v;}
 public String getSampleCode(){return sampleCode;} public void setSampleCode(String v){sampleCode=v;}
 public Enums.SampleType getSampleType(){return sampleType;} public void setSampleType(Enums.SampleType v){sampleType=v;}
 public Enums.Layer getLayer(){return layer;} public void setLayer(Enums.Layer v){layer=v;}
 public Instant getSampledAt(){return sampledAt;} public void setSampledAt(Instant v){sampledAt=v;}
 public Instant getReceivedAt(){return receivedAt;} public void setReceivedAt(Instant v){receivedAt=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
 public List<PressureReading> getPressureReadings(){return pressureReadings;}
 public List<ParticleDistribution> getParticleDistributions(){return particleDistributions;}
}
