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

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findBySegmentId(UUID segmentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id=:id")
    Optional<Review> lockById(UUID id);
}
