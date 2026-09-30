package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleRepository extends JpaRepository<Sample, Long> {
    java.util.Optional<Sample> findBySampleCode(String sampleCode);
    java.util.List<Sample> findByBatchIdOrderBySampledAtAsc(Long batchId);
}
