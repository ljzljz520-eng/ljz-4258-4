package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    java.util.Optional<Review> findByComparisonId(Long comparisonId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.comparisonId = :comparisonId")
    java.util.Optional<Review> findByComparisonIdForUpdate(@Param("comparisonId") Long comparisonId);
}
