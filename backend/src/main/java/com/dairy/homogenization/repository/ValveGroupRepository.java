package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ValveGroupRepository extends JpaRepository<ValveGroup, Long> {
    java.util.Optional<ValveGroup> findByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ValveGroup v where v.id = :id")
    java.util.Optional<ValveGroup> findByIdForUpdate(@Param("id") Long id);
}
