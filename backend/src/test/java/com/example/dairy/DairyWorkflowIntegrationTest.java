package com.example.dairy;

import com.example.dairy.domain.*;
import com.example.dairy.dto.Dtos.*;
import com.example.dairy.repository.*;
import com.example.dairy.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DairyWorkflowIntegrationTest extends AbstractIntegrationTest {
    static final UUID OK = UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID LATE = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID ALG = UUID.fromString("00000000-0000-0000-0000-000000000102");
    static final UUID LAYER = UUID.fromString("00000000-0000-0000-0000-000000000103");
    static final UUID POINT = UUID.fromString("00000000-0000-0000-0000-000000000040");

    @Autowired PairingService pairing;
    @Autowired PressureCorrectionService correction;
    @Autowired EvidenceService evidence;
    @Autowired ImportService imports;
    @Autowired SegmentService segments;
    @Autowired BatchSegmentRepository segmentRepository;
    @Autowired ComparisonRepository comparisonRepository;
    @Autowired ImportFileRepository importFileRepository;

    @Test
    void pressureInterchangeMarksDependentComparisonsStaleAndBlocksEvidenceUntilReconfirmed() {
        pairing.autoPair(OK, "operator");
        Comparison first = comparisonRepository.findBySegmentIdOrderByProposedAtDesc(OK).stream().findFirst().orElseThrow();
        pairing.confirm(first.getId(), "lab");

        PressureMappingResponse mapping = correction.correct(OK, new CorrectionRequest(
                POINT, "P-HIGH", "HIGH", 1, new java.math.BigDecimal("1.020000"),
                new java.math.BigDecimal("-0.500000"), "interchange calibration"), "lab");
        assertEquals(1, mapping.correctionLevel());
        Comparison stale = comparisonRepository.findById(first.getId()).orElseThrow();
        assertEquals(Enums.ComparisonStatus.STALE, stale.getStatus());
        assertTrue(stale.getReasons().stream().anyMatch(r -> r.getCode().equals("PRESSURE_MAPPING_CHANGED")));
        assertThrows(RuntimeException.class, () -> evidence.lockEvidence(OK, "reviewer"));

        Comparison refreshed = pairing.refresh(first.getId());
        assertNotEquals(Enums.ComparisonStatus.STALE, refreshed.getStatus());
        pairing.confirm(first.getId(), "lab");
        assertDoesNotThrow(() -> evidence.lockEvidence(OK, "reviewer"));
    }

    @Test
    void particleAlgorithmVersionChangeIsShownBesideCurveAndCannotConfirm() {
        pairing.autoPair(ALG, "operator");
        Comparison c = comparisonRepository.findBySegmentIdOrderByProposedAtDesc(ALG).stream().findFirst().orElseThrow();
        assertEquals(Enums.ComparisonStatus.BLOCKED, c.getStatus());
        assertTrue(c.getReasons().stream().anyMatch(r -> r.getCode().equals("PARTICLE_ALGORITHM_MISMATCH")));
        assertThrows(RuntimeException.class, () -> pairing.confirm(c.getId(), "lab"));
    }

    @Test
    void sampleLayeringBlocksComparison() {
        pairing.autoPair(LAYER, "operator");
        Comparison c = comparisonRepository.findBySegmentIdOrderByProposedAtDesc(LAYER).stream().findFirst().orElseThrow();
        assertEquals(Enums.ComparisonStatus.BLOCKED, c.getStatus());
        assertTrue(c.getReasons().stream().anyMatch(r -> r.getCode().equals("LAYER_MISMATCH")));
    }

    @Test
    void lateBeforeSampleIsPairedBySampledTimeRatherThanArrivalTime() {
        pairing.autoPair(LATE, "operator");
        List<Comparison> comparisons = comparisonRepository.findBySegmentIdOrderByProposedAtDesc(LATE);
        assertEquals(1, comparisons.size());
        assertEquals("LATE-BEFORE", comparisons.get(0).getBeforeSample().getSampleCode());
        assertEquals("LATE-AFTER", comparisons.get(0).getAfterSample().getSampleCode());
        assertDoesNotThrow(() -> pairing.confirm(comparisons.get(0).getId(), "lab"));
    }

    @Test
    void partialFileFailureDoesNotPretendCompleteSuccessAndRetriesWork() {
        String good = """
          sampleCode=RETRY-GOOD,measurementPointCode=HOM-MAIN,algorithmInstrumentCode=PSA-01,algorithmName=Fraunberger-LDD,algorithmVersion=1.8.2,sampleType=AFTER,layer=MIDDLE,sampledAt=2026-09-30T15:20:00Z
          stageOrder=1,rawLabel=P-LOW,rawBar=50,measuredAt=2026-09-30T15:19:30Z
          binUm=1.0,volumePct=5
          """;
        String bad = """
          sampleCode=RETRY-BAD,measurementPointCode=HOM-MAIN,algorithmInstrumentCode=PSA-01,algorithmName=Fraunberger-LDD,algorithmVersion=1.8.2,sampleType=AFTER,layer=MIDDLE,sampledAt=2026-09-30T15:21:00Z
          stageOrder=1,rawLabel=P-LOW,rawBar=50,measuredAt=2026-09-30T15:20:30Z
          """;
        MultipartFile f1 = new MockMultipartFile("files","good.csv","text/csv",good.getBytes());
        MultipartFile f2 = new MockMultipartFile("files","bad.csv","text/csv",bad.getBytes());
        ImportJob job = imports.createJob(LATE, List.of(f1,f2), "operator");
        imports.processPendingJobs(1, "worker");
        ImportJob processed = imports.getJob(job.getId());
        assertEquals(Enums.ImportStatus.COMPLETED_WITH_FAILURES, processed.getStatus());
        assertEquals(1, processed.getSuccessfulFiles()); assertEquals(1, processed.getFailedFiles());
        assertThrows(RuntimeException.class, () -> evidence.lockEvidence(LATE, "reviewer"));

        ImportFile failed = processed.getFiles().stream().filter(f -> f.getStatus()==Enums.FileStatus.FAILED).findFirst().orElseThrow();
        String fixed = bad.replace("sampleCode=RETRY-BAD", "sampleCode=RETRY-BAD") + "binUm=1.0,volumePct=4\n";
        failed.setContent(fixed.getBytes());
        importFileRepository.save(failed);
        imports.processPendingJobs(1, "worker");
        assertEquals(Enums.ImportStatus.SUCCEEDED, imports.getJob(job.getId()).getStatus());
    }

    @Test
    void correctionBeforeLockExpiresEvidenceAndLockedSegmentRejectsLaterCorrection() {
        pairing.autoPair(OK, "operator");
        Comparison c = comparisonRepository.findBySegmentIdOrderByProposedAtDesc(OK).stream().findFirst().orElseThrow();
        pairing.confirm(c.getId(), "lab");

        correction.correct(OK, new CorrectionRequest(POINT, "P-HIGH", "HIGH", 1,
                new java.math.BigDecimal("1.010000"), new java.math.BigDecimal("0.000000"), "before review"), "lab");
        assertThrows(RuntimeException.class, () -> evidence.lockEvidence(OK, "reviewer"));
        pairing.refresh(c.getId());
        pairing.confirm(c.getId(), "lab");
        evidence.lockEvidence(OK, "reviewer");
        assertThrows(RuntimeException.class, () -> correction.correct(OK, new CorrectionRequest(POINT, "P-HIGH", "HIGH", 2,
                new java.math.BigDecimal("1.030000"), new java.math.BigDecimal("0.000000"), "after lock"), "lab"));
    }

}
