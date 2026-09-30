package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparabilityFindingRepository extends JpaRepository<ComparabilityFinding, Long> { void deleteByComparisonId(Long comparisonId);
    java.util.List<ComparabilityFinding> findByComparisonIdOrderBySeverityDescIdAsc(Long comparisonId);
}
