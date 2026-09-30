package com.example.dairy;

import com.example.dairy.domain.Enums;
import com.example.dairy.dto.Dtos.CorrectionRequest;
import com.example.dairy.repository.BatchSegmentRepository;
import com.example.dairy.repository.ComparisonRepository;
import com.example.dairy.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Import(ConcurrentReviewCorrectionIntegrationTest.Helpers.class)
class ConcurrentReviewCorrectionIntegrationTest extends AbstractIntegrationTest {
    static final UUID OK = UUID.fromString("00000000-0000-0000-0000-000000000100");
    static final UUID POINT = UUID.fromString("00000000-0000-0000-0000-000000000040");

    @Autowired PairingService pairing;
    @Autowired EvidenceService evidence;
    @Autowired TxHelpers tx;
    @Autowired BatchSegmentRepository segments;
    @Autowired ComparisonRepository comparisons;

    @Test
    void onlyOneOfConcurrentReviewLockAndCorrectionCanWin() throws Exception {
        pairing.autoPair(OK, "operator");
        var c = comparisons.findBySegmentIdOrderByProposedAtDesc(OK).stream().findFirst().orElseThrow();
        pairing.confirm(c.getId(), "lab");

        int currentLevel = c.getPressureMapping().getCorrectionLevel();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger locks = new AtomicInteger();
        AtomicInteger corrections = new AtomicInteger();
        Future<?> f1 = pool.submit(() -> tx.withoutResult(() -> { start.await(); evidence.lockEvidence(OK, "reviewer"); locks.incrementAndGet(); }));
        Future<?> f2 = pool.submit(() -> tx.withoutResult(() -> { start.await(); Thread.sleep(50);
            correction(currentLevel + 1); corrections.incrementAndGet(); }));
        start.countDown();
        await(f1); await(f2); pool.shutdown();
        assertEquals(1, locks.get() + corrections.get(), "锁证与修正在同一批段上必须互斥");
        if (locks.get() == 1) {
            assertEquals(Enums.SegmentStatus.LOCKED, segments.findById(OK).orElseThrow().getStatus());
            assertThrows(Exception.class, () -> correction(currentLevel + 2));
        } else {
            assertEquals(Enums.SegmentStatus.OPEN, segments.findById(OK).orElseThrow().getStatus());
        }
    }

    private void correction(int level) {
        tx.service().correct(OK, new CorrectionRequest(POINT, "P-HIGH", "HIGH", level,
                new BigDecimal("1.030000"), BigDecimal.ZERO, "concurrent correction"), "lab");
    }
    private void await(Future<?> f) { try { f.get(20, TimeUnit.SECONDS); } catch (Exception ignored) { } }

    @TestConfiguration
    static class Helpers {
        @Bean
        TxHelpers txHelpers(PlatformTransactionManager tm, PressureCorrectionService service) { return new TxHelpers(tm, service); }
    }
    static class TxHelpers {
        private final TransactionTemplate template; private final PressureCorrectionService service;
        TxHelpers(PlatformTransactionManager tm, PressureCorrectionService service) { this.template=new TransactionTemplate(tm); this.service=service; }
        PressureCorrectionService service() { return service; }
        void withoutResult(ThrowingRunnable runnable) {
            template.executeWithoutResult(status -> { try { runnable.run(); } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new RuntimeException(e); } });
        }
    }
    interface ThrowingRunnable { void run() throws Exception; }
}
