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

public interface PressureReadingRepository extends JpaRepository<PressureReading, UUID> {
    @Query("select distinct r.mapping.id from PressureReading r where r.sample.segment.id=:segmentId and r.mapping is not null")
    List<UUID> findMappingIdsBySegment(UUID segmentId);
}
