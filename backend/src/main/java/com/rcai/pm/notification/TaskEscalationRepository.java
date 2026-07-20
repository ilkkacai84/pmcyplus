package com.rcai.pm.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaskEscalationRepository extends JpaRepository<TaskEscalation, Long> {
    Optional<TaskEscalation> findByTaskId(Long taskId);
}
