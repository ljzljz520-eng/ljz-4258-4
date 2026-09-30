package com.dairy.homogenization.service;

import com.dairy.homogenization.domain.*;
import com.dairy.homogenization.dto.Requests.*;
import com.dairy.homogenization.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorkflowIntegrationTest {
    @Autowired ImportService importService;
    @Autowired PressureMappingService mappingService;
    @Autowired ComparisonRuleApplicationService comparisonService;
    @Autowired ReviewService reviewService;
    @Autowired ProductBatchRepository batches;
    @Autowired ValveGroupRepository valveGroups;
    @Autowired SamplePointRepository points;
    @Autowired PressureMappingVersionRepository mappings;
    @Autowired StabilityWindowRepository windows;
    @Autowired InstrumentAlgorithmVersionRepository algorithms;
    @Autowired SampleRepository samples;
    @Autowired ComparisonCandidateRepository comparisons;
    @Autowired ReviewRepository reviews;
    @Autowired EvidenceSnapshotRepository snapshots;
    @Autowired ImportJobRepository jobs;
    @Autowired ImportFileRepository importFiles;

    @BeforeEach
    void seed() {
        importFiles.deleteAll(); jobs.deleteAll(); snapshots.deleteAll(); reviews.deleteAll(); comparisons.deleteAll();
        samples.deleteAll(); windows.deleteAll(); points.deleteAll(); mappings.deleteAll();
        algorithms.deleteAll(); batches.deleteAll(); valveGroups.deleteAll();
        ValveGroup vg = new ValveGroup(); vg.setCode("VG-T"); vg.setName("测试阀组"); valveGroups.save(vg);
        SamplePoint before = new SamplePoint(); before.setCode("T-BEFORE"); before.setName("前");
        before.setValveGroupId(vg.getId()); before.setSamplingLine("LINE-T"); points.save(before);
        SamplePoint after = new SamplePoint(); after.setCode("T-AFTER"); after.setName("后");
        after.setValveGroupId(vg.getId()); after.setSamplingLine("LINE-T"); points.save(after);
        ProductBatch b = new ProductBatch(); b.setBatchNumber("B-T"); b.setProductCode("MILK");
        b.setProductName("奶"); b.setLayerName("BULK");
        b.setStartedAt(OffsetDateTime.parse("2026-09-30T08:00:00Z")); batches.save(b);
        PressureMappingVersion m = new PressureMappingVersion(); m.setValveGroupId(vg.getId());
        m.setVersionNumber(1); m.setStatus(MappingStatus.ACTIVE);
        m.setEffectiveFrom(OffsetDateTime.parse("2026-09-30T00:00:00Z")); m.setChangeSummary("initial");
        addItem(m,"P1","STAGE_180",180,1); addItem(m,"P2","STAGE_220",220,2); mappings.save(m);
        for (PressureMappingItem i : m.getItems()) i.setPressureMappingVersion(m);
        mappings.save(m);
        window(b.getId(), vg.getId(), "STAGE_180", "09:00", "09:30", 180);
        window(b.getId(), vg.getId(), "STAGE_220", "09:30", "10:00", 220);
        algorithm("PSD-1.0", AlgorithmStatus.ACTIVE, "2026-01-01T00:00:00Z");
    }

    @Test
    void pressureSwapExpiresUnlockedComparisonButApprovedSnapshotStaysLocked() {
        importValidPressureAndParticles("09:10", "09:09", "09:11", "B1", "A1", "PSD-1.0", "BULK");
        comparisonService.evaluateBatch(batchId(), true);
        ComparisonCandidate c = onlyComparable();
        comparisonService.confirm(c.getId());
        reviewService.requestReview(c.getId(), "analyst");
        reviewService.decide(c.getId(), true, "ok", "reviewer");
        String oldFingerprint = comparisons.findById(c.getId()).orElseThrow().getFingerprint();
        Long snapshotCount = snapshots.count();

        mappingService.correct(new CorrectMappingRequest(vgId(), List.of(
                new MappingItemInput("P1","STAGE_220",new BigDecimal("220"),2),
                new MappingItemInput("P2","STAGE_180",new BigDecimal("180"),1)), "swap"), "analyst");

        ComparisonCandidate approved = comparisons.findById(c.getId()).orElseThrow();
        assertEquals(ComparisonStatus.APPROVED, approved.getStatus());
        assertEquals(oldFingerprint, approved.getFingerprint());
        assertEquals(snapshotCount, snapshots.count());
    }

    @Test
    void particleAlgorithmChangeMakesCandidateIncomparable() {
        algorithm("PSD-2.0", AlgorithmStatus.ACTIVE, "2026-01-01T00:00:00Z");
        importValidPressureAndParticles("09:10", "09:09", "09:11", "B-OLD", "A-OLD", "PSD-1.0", "BULK");
        importParticleCsv("""
            batch_number,sample_code,sample_point,position,layer,sampled_at,received_at,instrument,algorithm_version,measured_at,bin_size_um,volume_fraction,cumulative_fraction,d10_um,d50_um,d90_um,mean_um
            B-T,A-NEW,T-AFTER,AFTER,BULK,2026-09-30T09:11:00Z,2026-09-30T09:40:00Z,PSA-900,PSD-2.0,2026-09-30T09:20:00Z,1,0.4,0.4,1,2,4,2
            B-T,A-NEW,T-AFTER,AFTER,BULK,2026-09-30T09:11:00Z,2026-09-30T09:40:00Z,PSA-900,PSD-2.0,2026-09-30T09:20:00Z,2,0.6,1.0,1,2,4,2
            """);
        comparisonService.evaluateBatch(batchId(), true);
        ComparisonCandidate changed = comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId()).stream()
                .filter(x -> x.getAfterSampleId().equals(sample("A-NEW").getId())).findFirst().orElseThrow();
        assertFalse(changed.isComparable());
        assertTrue(changed.getReasonSummary().contains("粒度仪器算法版本不一致"));
    }

    @Test
    void createsOneToManyCandidatesAndBlocksCrossLayerPair() {
        importValidPressureAndParticles("09:10", "09:09", "09:11", "B1", "A1", "PSD-1.0", "BULK");
        importParticleCsv("""
            batch_number,sample_code,sample_point,position,layer,sampled_at,received_at,instrument,algorithm_version,measured_at,bin_size_um,volume_fraction,cumulative_fraction,d10_um,d50_um,d90_um,mean_um
            B-T,A2,T-AFTER,AFTER,BULK,2026-09-30T09:12:00Z,2026-09-30T09:40:00Z,PSA-900,PSD-1.0,2026-09-30T09:20:00Z,1,0.4,0.4,1,2,4,2
            B-T,A2,T-AFTER,AFTER,BULK,2026-09-30T09:12:00Z,2026-09-30T09:40:00Z,PSA-900,PSD-1.0,2026-09-30T09:20:00Z,2,0.6,1.0,1,2,4,2
            B-T,ATOP,T-AFTER,AFTER,TOP,2026-09-30T09:13:00Z,2026-09-30T09:40:00Z,PSA-900,PSD-1.0,2026-09-30T09:20:00Z,1,0.4,0.4,1,2,4,2
            B-T,ATOP,T-AFTER,AFTER,TOP,2026-09-30T09:13:00Z,2026-09-30T09:40:00Z,PSA-900,PSD-1.0,2026-09-30T09:20:00Z,2,0.6,1.0,1,2,4,2
            """);
        comparisonService.createManualPair(sample("B1").getId(), sample("ATOP").getId(), "STAGE_180", "analyst");
        comparisonService.evaluateBatch(batchId(), true);
        List<ComparisonCandidate> all = comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId());
                assertEquals(2, all.stream().filter(c -> c.isComparable()).count());
        ComparisonCandidate top = all.stream().filter(c -> c.getAfterSampleId().equals(sample("ATOP").getId())).findFirst().orElseThrow();
        assertFalse(top.isComparable());
        assertTrue(top.getReasonSummary().contains("样品分层不一致"));
    }

    @Test
    void partialFileFailureDoesNotPretendCompleteAndRetryRepairsJob() {
        String pressure = "batch_number,valve_group,raw_label,pressure_bar,observed_at\nB-T,VG-T,P1,180,2026-09-30T09:10:00Z\n";
        String badParticle = particleContent("BRETRY","T-BEFORE","BEFORE","BULK","09:12","PSD-FUTURE");
        ImportJob pressureJob = importService.createJob(List.of(
                new MockMultipartFile("files","good-pressure.csv","text/csv",pressure.getBytes())),
                "PRESSURE_CSV", "operator");
        importService.processNextJob();
        assertEquals(ImportJobStatus.COMPLETED, jobs.findById(pressureJob.getId()).orElseThrow().getStatus());
        ImportJob job = importService.createJob(List.of(
                new MockMultipartFile("files","good-particle.csv","text/csv",
                        particleContent("B1","T-BEFORE","BEFORE","BULK","09:09","PSD-1.0").getBytes()),
                new MockMultipartFile("files","bad-particle.csv","text/csv",badParticle.getBytes())),
                "PARTICLE_CSV", "operator");
        importService.processNextJob();
        ImportJob failed = jobs.findById(job.getId()).orElseThrow();
        assertEquals(ImportJobStatus.PARTIAL_SUCCESS, failed.getStatus());
        assertEquals(1, failed.getSucceededFiles());
        assertEquals(1, failed.getFailedFiles());
        // Registering the missing algorithm and retrying the exact file repairs the job atomically.
        algorithm("PSD-FUTURE", AlgorithmStatus.ACTIVE, "2026-01-01T00:00:00Z");
        importService.retryFile(importFiles.findByJobIdOrderByIdAsc(job.getId()).get(1).getId());
        assertEquals(ImportJobStatus.COMPLETED, jobs.findById(job.getId()).orElseThrow().getStatus());
        importParticleCsv(particleContent("A1","T-AFTER","AFTER","BULK","09:11","PSD-1.0"));
        comparisonService.evaluateBatch(batchId(), true);
        ComparisonCandidate c = onlyComparable();
        assertTrue(c.isComparable());
    }

    @Test
    void concurrentReviewAndCorrectionSerializesWithoutDuplicateSnapshots() throws Exception {
        importValidPressureAndParticles("09:10", "09:09", "09:11", "BC", "AC", "PSD-1.0", "BULK");
        comparisonService.evaluateBatch(batchId(), true);
        ComparisonCandidate c = onlyComparable();
        comparisonService.confirm(c.getId());
        reviewService.requestReview(c.getId(), "analyst");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> review = () -> { start.await(); reviewService.decide(c.getId(),true,"ok","reviewer"); return "review"; };
        Callable<String> correct = () -> { start.await(); mappingService.correct(new CorrectMappingRequest(vgId(),List.of(
                new MappingItemInput("P1","STAGE_220",new BigDecimal("220"),2),
                new MappingItemInput("P2","STAGE_180",new BigDecimal("180"),1)), "swap"), "analyst"); return "correct"; };
        Future<String> f1=pool.submit(review); Future<String> f2=pool.submit(correct); start.countDown();
        Set<String> done = new HashSet<>();
        done.add(f1.get(30,TimeUnit.SECONDS));
        try { done.add(f2.get(30,TimeUnit.SECONDS)); } catch (ExecutionException expected) { done.add("correct-blocked"); }
        pool.shutdownNow();
        assertEquals(1, snapshots.count());
        assertTrue(done.contains("review"));
    }


    @Test
    void lateBeforeSampleIsAutoCandidatedButBlocked() {
        importPressureCsv("09:10");
        importParticleCsv(particleContent("BLATE","T-BEFORE","BEFORE","BULK","09:14","PSD-1.0") +
                particleContent("AEARLY","T-AFTER","AFTER","BULK","09:11","PSD-1.0").split("\n",2)[1]);
        comparisonService.evaluateBatch(batchId(), true);
        ComparisonCandidate c = comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId()).stream()
                .filter(x -> x.getBeforeSampleId().equals(sample("BLATE").getId())).findFirst().orElseThrow();
        assertFalse(c.isComparable());
        assertTrue(c.getReasonSummary().contains("迟到前样"));
    }

    private void importValidPressureAndParticles(String pressureAt, String beforeAt, String afterAt,
                                                 String beforeCode, String afterCode, String alg, String layer) {
        importPressureCsv(pressureAt);
        importParticleCsv(particleContent(beforeCode,"T-BEFORE","BEFORE",layer,beforeAt,alg) +
                particleContent(afterCode,"T-AFTER","AFTER",layer,afterAt,alg).split("\n",2)[1]);
    }
    private void importValidParticlesOnly(String b, String a, String at) {
        importParticleCsv(particleContent(b,"T-BEFORE","BEFORE","BULK",at,"PSD-1.0") +
                particleContent(a,"T-AFTER","AFTER","BULK",at,"PSD-1.0").split("\n",2)[1]);
    }
    private void importPressureCsv(String at) {
        importService.createJob(List.of(new MockMultipartFile("files","p-"+at+".csv","text/csv",("""
            batch_number,valve_group,raw_label,pressure_bar,observed_at
            B-T,VG-T,P1,180,2026-09-30T%s:00Z
            """.formatted(at)).getBytes())), "PRESSURE_CSV","operator");
        importService.processNextJob();
        assertEquals(ImportJobStatus.COMPLETED, jobs.findAll().get(0).getStatus());
    }
    private void importParticleCsv(String csv) {
        importService.createJob(List.of(new MockMultipartFile("files","m.csv","text/csv",csv.getBytes())),
                "PARTICLE_CSV","operator");
        importService.processNextJob();
    }
    private String particleContent(String code,String point,String position,String layer,String at,String alg) {
        return """
            batch_number,sample_code,sample_point,position,layer,sampled_at,received_at,instrument,algorithm_version,measured_at,bin_size_um,volume_fraction,cumulative_fraction,d10_um,d50_um,d90_um,mean_um
            B-T,%s,%s,%s,%s,2026-09-30T%s:00Z,2026-09-30T09:40:00Z,PSA-900,%s,2026-09-30T09:20:00Z,1,0.4,0.4,1,2,4,2
            B-T,%s,%s,%s,%s,2026-09-30T%s:00Z,2026-09-30T09:40:00Z,PSA-900,%s,2026-09-30T09:20:00Z,2,0.6,1.0,1,2,4,2
            """.formatted(code,point,position,layer,at,alg,code,point,position,layer,at,alg);
    }
    private ComparisonCandidate onlyComparable() {
        return comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId()).stream()
                .filter(ComparisonCandidate::isComparable).reduce((a,b)->{throw new AssertionError("expected one");}).orElseThrow();
    }
    private ComparisonCandidate onlyComparableOrIncomplete() {
        return comparisons.findByBatchIdOrderByStageCodeAscIdAsc(batchId()).stream().findFirst().orElseThrow();
    }
    private Long batchId(){return batches.findByBatchNumber("B-T").orElseThrow().getId();}
    private Long vgId(){return valveGroups.findByCode("VG-T").orElseThrow().getId();}
    private Sample sample(String code){return samples.findBySampleCode(code).orElseThrow();}
    private void addItem(PressureMappingVersion m,String raw,String stage,int pressure,int order){
        PressureMappingItem i=new PressureMappingItem(); i.setPressureMappingVersion(m); i.setRawLabel(raw);
        i.setCalibratedStageCode(stage); i.setNominalPressureBar(new BigDecimal(pressure)); i.setDisplayOrder(order);
        m.getItems().add(i);
    }
    private void window(Long batch,Long vg,String stage,String start,String end,int p){
        StabilityWindow w=new StabilityWindow(); w.setBatchId(batch); w.setValveGroupId(vg); w.setStageCode(stage);
        w.setStartedAt(OffsetDateTime.parse("2026-09-30T"+start+":00Z")); w.setEndedAt(OffsetDateTime.parse("2026-09-30T"+end+":00Z"));
        w.setNominalPressureBar(new BigDecimal(p)); windows.save(w);
    }
    private void algorithm(String v,AlgorithmStatus status,String from){
        InstrumentAlgorithmVersion a=new InstrumentAlgorithmVersion(); a.setInstrumentCode("PSA-900");
        a.setMeasurementKind("PARTICLE_SIZE"); a.setAlgorithmVersion(v); a.setStatus(status);
        a.setEffectiveFrom(OffsetDateTime.parse(from)); algorithms.save(a);
    }
}
