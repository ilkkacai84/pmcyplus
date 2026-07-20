package com.rcai.pm.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    List<TaskItem> findByProjectIdAndMergedIntoIdIsNullOrderByCreatedAtAsc(Long projectId);
    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
    long countByOwnerIdAndStatusNotIn(Long ownerId, List<TaskStatus> statuses);
    long countByOwnerIdAndPlannedEndAtBeforeAndStatusNotIn(Long ownerId, LocalDateTime now, List<TaskStatus> statuses);
}
