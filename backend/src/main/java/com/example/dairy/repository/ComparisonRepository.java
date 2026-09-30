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

public interface ComparisonRepository extends JpaRepository<Comparison, UUID> {
    List<Comparison> findBySegmentIdOrderByProposedAtDesc(UUID segmentId);
    List<Comparison> findBySegmentIdAndStatusNotOrderByProposedAtDesc(UUID segmentId, Enums.ComparisonStatus status);
    Optional<Comparison> findFirstByAfterSample_IdAndStatusIn(UUID afterSampleId, List<Enums.ComparisonStatus> statuses);
    @Query("select c from Comparison c join c.dependency d where d.pressureMapping.id = :mappingId and c.status <> :superseded")
    List<Comparison> findActiveByDependencyMappingId(UUID mappingId, Enums.ComparisonStatus superseded);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comparison c where c.id = :id")
    Optional<Comparison> lockById(UUID id);
}
