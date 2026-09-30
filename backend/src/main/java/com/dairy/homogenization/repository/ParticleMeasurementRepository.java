package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticleMeasurementRepository extends JpaRepository<ParticleMeasurement, Long> {
    java.util.Optional<ParticleMeasurement> findBySampleIdAndStatus(Long sampleId, com.dairy.homogenization.domain.MeasurementStatus status);
}
