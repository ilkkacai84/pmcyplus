package com.rcai.pm.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryVersionRepository extends JpaRepository<DeliveryVersion, Long> {
    @Query("select coalesce(max(d.versionNo), 0) from DeliveryVersion d where d.task.id = :taskId")
    int findMaxVersionNo(@Param("taskId") Long taskId);

    Optional<DeliveryVersion> findFirstByTaskIdAndStatusOrderByVersionNoDesc(Long taskId, DeliveryStatus status);

    @Query("select d from DeliveryVersion d where d.task.project.id = :projectId order by d.task.id, d.versionNo desc")
    List<DeliveryVersion> findByProjectId(@Param("projectId") Long projectId);
}
