package com.rcai.pm.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    List<TaskItem> findByProjectIdAndMergedIntoIdIsNullOrderByCreatedAtAsc(Long projectId);
    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
    long countByOwnerIdAndStatusNotIn(Long ownerId, List<TaskStatus> statuses);
    long countByOwnerIdAndPlannedEndAtBeforeAndStatusNotIn(Long ownerId, LocalDateTime now, List<TaskStatus> statuses);
    @EntityGraph(attributePaths = {"project", "owner"})
    @Query("select task from TaskItem task where task.owner.department.id = :departmentId " +
        "and task.mergedIntoId is null order by task.plannedEndAt asc")
    List<TaskItem> findDepartmentTasks(@Param("departmentId") Long departmentId);
}
