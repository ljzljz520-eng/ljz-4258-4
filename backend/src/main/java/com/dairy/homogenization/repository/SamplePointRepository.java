package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SamplePointRepository extends JpaRepository<SamplePoint, Long> {
    java.util.Optional<SamplePoint> findByCode(String code);
}
