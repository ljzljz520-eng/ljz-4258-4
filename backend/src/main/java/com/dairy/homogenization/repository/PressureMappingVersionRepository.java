package com.dairy.homogenization.repository;

import com.dairy.homogenization.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PressureMappingVersionRepository extends JpaRepository<PressureMappingVersion, Long> {
    Optional<PressureMappingVersion> findByValveGroupIdAndStatus(Long valveGroupId, MappingStatus status);
    @Query("select max(m.versionNumber) from PressureMappingVersion m where m.valveGroupId = :valveGroupId")
    Optional<Integer> findMaxVersion(@Param("valveGroupId") Long valveGroupId);
    List<PressureMappingVersion> findByValveGroupIdOrderByVersionNumberDesc(Long valveGroupId);
}
