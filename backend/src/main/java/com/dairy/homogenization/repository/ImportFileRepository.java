package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportFileRepository extends JpaRepository<ImportFile, Long> {
    java.util.List<ImportFile> findByJobIdOrderByIdAsc(Long jobId);
    java.util.List<ImportFile> findByJobIdAndStatusInOrderByIdAsc(Long jobId, java.util.Collection<com.dairy.homogenization.domain.ImportFileStatus> statuses);
    java.util.Optional<ImportFile> findByChecksumSha256AndStatus(String checksum, com.dairy.homogenization.domain.ImportFileStatus status);
}
