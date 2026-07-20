package com.rcai.pm.workflow;

import com.rcai.pm.project.ProjectType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowApprovalStepRepository extends JpaRepository<WorkflowApprovalStep, Long> {
    @EntityGraph(attributePaths = "template")
    List<WorkflowApprovalStep> findByTemplateProjectTypeAndActiveTrueOrderByStepOrderAsc(ProjectType projectType);
    List<WorkflowApprovalStep> findByTemplateIdOrderByStepOrderAsc(Long templateId);
    long countByTemplateId(Long templateId);
}
