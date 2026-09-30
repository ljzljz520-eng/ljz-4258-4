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

public interface PressureCalibrationMappingRepository extends JpaRepository<PressureCalibrationMapping, UUID> {
    Optional<PressureCalibrationMapping> findFirstByValveGroup_IdAndMeasurementPoint_IdAndInstrumentLabelAndActiveOrderByVersionDesc(
            UUID valveGroupId, UUID pointId, String label, boolean active);
    @Query("""
      select m from PressureCalibrationMapping m where m.valveGroup.id=:valveId
      and m.measurementPoint.id=:pointId and m.instrumentLabel in :labels and m.active=true
      order by m.version desc
    """)
    List<PressureCalibrationMapping> findActive(UUID valveId, UUID pointId, List<String> labels);
    @Query("select coalesce(max(m.version),0)+1 from PressureCalibrationMapping m where m.valveGroup.id=:v and m.measurementPoint.id=:p and m.instrumentLabel=:l")
    int nextVersion(UUID v, UUID p, String l);
}
