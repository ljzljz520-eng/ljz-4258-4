package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ComparisonCandidateRepository extends JpaRepository<ComparisonCandidate, Long> {
    java.util.Optional<ComparisonCandidate> findByBeforeSampleIdAndAfterSampleId(Long beforeSampleId, Long afterSampleId);
    java.util.List<ComparisonCandidate> findByBatchIdOrderByStageCodeAscIdAsc(Long batchId);
    java.util.List<ComparisonCandidate> findByValveGroupIdAndStatusNot(Long valveGroupId, com.dairy.homogenization.domain.ComparisonStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ComparisonCandidate c where c.id = :id")
    java.util.Optional<ComparisonCandidate> findByIdForUpdate(@Param("id") Long id);
}
