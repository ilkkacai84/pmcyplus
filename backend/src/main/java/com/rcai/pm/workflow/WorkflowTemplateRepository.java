package com.rcai.pm.workflow;

import com.rcai.pm.project.ProjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {
    Optional<WorkflowTemplate> findByProjectTypeAndActiveTrue(ProjectType projectType);
    List<WorkflowTemplate> findAllByOrderByProjectTypeAsc();
}
