package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {
    java.util.Optional<ProductBatch> findByBatchNumber(String batchNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ProductBatch b where b.id = :id")
    java.util.Optional<ProductBatch> findByIdForUpdate(@Param("id") Long id);
}
