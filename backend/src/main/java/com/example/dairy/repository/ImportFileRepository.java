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

public interface ImportFileRepository extends JpaRepository<ImportFile, UUID> {
    boolean existsByChecksumAndStatus(String checksum, Enums.FileStatus status);
    Optional<ImportFile> findFirstByChecksumAndStatus(String checksum, Enums.FileStatus status);
}
