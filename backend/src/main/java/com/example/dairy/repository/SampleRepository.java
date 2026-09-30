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

public interface SampleRepository extends JpaRepository<Sample, UUID> {
    List<Sample> findBySegmentIdOrderBySampledAtAsc(UUID segmentId);
    List<Sample> findBySegmentIdAndSampleTypeOrderBySampledAtAsc(UUID segmentId, Enums.SampleType type);
    Optional<Sample> findBySampleCode(String code);
}
