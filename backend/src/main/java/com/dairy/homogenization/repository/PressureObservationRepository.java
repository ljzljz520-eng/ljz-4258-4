package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PressureObservationRepository extends JpaRepository<PressureObservation, Long> {
    java.util.List<PressureObservation> findByBatchIdAndValveGroupIdOrderByObservedAtAsc(Long batchId, Long valveGroupId);
}
