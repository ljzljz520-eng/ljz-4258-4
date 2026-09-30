package com.example.dairy.repository;

import com.example.dairy.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
    List<ImportJob> findBySegmentIdOrderByCreatedAtAsc(UUID segmentId);
    @Query("""
      select j from ImportJob j where j.status in ('PENDING','RUNNING','FAILED','COMPLETED_WITH_FAILURES')
      and (j.lockedAt is null or j.lockedAt < :staleAt)
      order by j.createdAt
    """)
    List<ImportJob> findClaimable(Instant staleAt, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from ImportJob j where j.id=:id")
    Optional<ImportJob> lockById(UUID id);
}
