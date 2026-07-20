package com.rcai.pm.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface WorklogRepository extends JpaRepository<Worklog, Long> {
    List<Worklog> findByUserIdInAndWorkedOnBetween(List<Long> userIds, LocalDate from, LocalDate to);
}
