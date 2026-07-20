package com.rcai.pm.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "department")
    List<UserAccount> findAllByOrderByDisplayNameAsc();

    @EntityGraph(attributePaths = "department")
    @Query("select user from UserAccount user where user.department.id = :departmentId order by user.displayName asc")
    List<UserAccount> findDepartmentUsers(@Param("departmentId") Long departmentId);
}
