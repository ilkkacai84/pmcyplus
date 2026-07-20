package com.rcai.pm.requirement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    boolean existsByRequirementNo(String requirementNo);

    @Query("select r from Requirement r left join r.project project where r.submitter.id = :userId " +
           "or r.customer.id = :userId or r.assignee.id = :userId or project.manager.id = :userId " +
           "order by r.createdAt desc")
    List<Requirement> findAccessible(@Param("userId") Long userId);

    long countByStatus(RequirementStatus status);
}
