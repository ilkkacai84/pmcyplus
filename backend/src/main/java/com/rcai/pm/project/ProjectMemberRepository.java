package com.rcai.pm.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
