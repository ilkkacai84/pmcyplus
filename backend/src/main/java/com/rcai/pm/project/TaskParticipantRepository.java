package com.rcai.pm.project;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskParticipantRepository extends JpaRepository<TaskParticipant, TaskParticipantId> {
    @EntityGraph(attributePaths = "user")
    List<TaskParticipant> findByTaskIdOrderByUserId(Long taskId);
}
