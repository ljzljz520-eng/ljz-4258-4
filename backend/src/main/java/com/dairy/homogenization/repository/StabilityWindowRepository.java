package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StabilityWindowRepository extends JpaRepository<StabilityWindow, Long> {
    java.util.List<StabilityWindow> findByBatchIdAndValveGroupIdOrderByStartedAtAsc(Long batchId, Long valveGroupId);
}
