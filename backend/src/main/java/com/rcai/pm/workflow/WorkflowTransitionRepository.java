package com.rcai.pm.workflow;

import com.rcai.pm.project.ProjectType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    @EntityGraph(attributePaths = {"template", "allowedRoles"})
    Optional<WorkflowTransition> findByTemplateProjectTypeAndObjectTypeAndFromStatusAndToStatus(
        ProjectType projectType, WorkflowObjectType objectType, String fromStatus, String toStatus
    );

    @EntityGraph(attributePaths = {"template", "allowedRoles"})
    List<WorkflowTransition> findByTemplateProjectTypeAndObjectTypeAndFromStatusOrderByToStatus(
        ProjectType projectType, WorkflowObjectType objectType, String fromStatus
    );

    @EntityGraph(attributePaths = {"template", "allowedRoles"})
    List<WorkflowTransition> findByTemplateIdOrderByObjectTypeAscFromStatusAscToStatusAsc(Long templateId);
}
