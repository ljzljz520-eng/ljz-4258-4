package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    @Query("""
        select j from ImportJob j
        where j.status in (com.dairy.homogenization.domain.ImportJobStatus.RECEIVED,
                           com.dairy.homogenization.domain.ImportJobStatus.PROCESSING)
        order by j.createdAt
    """)
    List<ImportJob> findActive();

    @Query("""
        select count(j) from ImportJob j
        where j.status in (com.dairy.homogenization.domain.ImportJobStatus.RECEIVED,
                           com.dairy.homogenization.domain.ImportJobStatus.PROCESSING)
        and j.uploadedBy = :user
    """)
    long countActiveForUploader(@Param("user") String user);
}
