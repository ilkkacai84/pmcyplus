package com.rcai.pm.requirement;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {
    @EntityGraph(attributePaths = {"requirement", "template", "submittedBy"})
    Optional<ApprovalInstance> findFirstByRequirementIdOrderByCreatedAtDesc(Long requirementId);

    @EntityGraph(attributePaths = {"requirement", "template", "submittedBy"})
    List<ApprovalInstance> findByRequirementIdOrderByCreatedAtDesc(Long requirementId);
}
