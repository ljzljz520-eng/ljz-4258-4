package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentAlgorithmVersionRepository extends JpaRepository<InstrumentAlgorithmVersion, Long> {
    java.util.Optional<InstrumentAlgorithmVersion> findByInstrumentCodeAndMeasurementKindAndAlgorithmVersion(
        String instrumentCode, String measurementKind, String algorithmVersion);
    java.util.Optional<InstrumentAlgorithmVersion> findByInstrumentCodeAndMeasurementKindAndStatus(
        String instrumentCode, String measurementKind, com.dairy.homogenization.domain.AlgorithmStatus status);
}
