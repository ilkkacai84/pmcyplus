package com.rcai.pm.requirement;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    @EntityGraph(attributePaths = "actor")
    List<ApprovalAction> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);
}
