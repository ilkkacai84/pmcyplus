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

    @EntityGraph(attributePaths = {"project", "project.manager", "owner", "owner.department", "owner.department.manager"})
    @Query("select task from TaskItem task where task.plannedEndAt < :now and task.status not in :closedStatuses " +
        "and task.mergedIntoId is null")
    List<TaskItem> findOverdue(@Param("now") LocalDateTime now, @Param("closedStatuses") List<TaskStatus> closedStatuses);

    @EntityGraph(attributePaths = {"project", "owner", "owner.department"})
    @Query("select task from TaskItem task where task.owner.id in :ownerIds and task.plannedStartAt is not null " +
        "and task.plannedEndAt is not null and task.plannedStartAt <= :to and task.plannedEndAt >= :from " +
        "and task.status not in :closed")
    List<TaskItem> findResourceTasks(@Param("ownerIds") List<Long> ownerIds, @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to, @Param("closed") List<TaskStatus> closed);
}
