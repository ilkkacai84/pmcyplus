package com.rcai.pm.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByCode(String code);

    @Query("select distinct p from Project p where p.manager.id = :userId or p.customer.id = :userId or p.id in " +
           "(select pm.projectId from ProjectMember pm where pm.userId = :userId) order by p.updatedAt desc")
    List<Project> findAccessible(@Param("userId") Long userId);
}
