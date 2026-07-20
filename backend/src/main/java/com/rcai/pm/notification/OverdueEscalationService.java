package com.rcai.pm.notification;

import com.rcai.pm.project.TaskItem;
import com.rcai.pm.project.TaskItemRepository;
import com.rcai.pm.project.TaskStatus;
import com.rcai.pm.user.Department;
import com.rcai.pm.user.Role;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class OverdueEscalationService {
    private final TaskItemRepository tasks;
    private final TaskEscalationRepository escalations;
    private final UserAccountRepository users;
    private final NotificationService notifications;

    public OverdueEscalationService(TaskItemRepository tasks, TaskEscalationRepository escalations,
                                    UserAccountRepository users, NotificationService notifications) {
        this.tasks = tasks; this.escalations = escalations; this.users = users; this.notifications = notifications;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void run() { process(LocalDateTime.now(), Instant.now()); }

    @Transactional
    public int process(LocalDateTime now, Instant notifiedAt) {
        int count = 0;
        for (TaskItem task : tasks.findOverdue(now,
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.MERGED))) {
            long hours = Math.max(0, Duration.between(task.getPlannedEndAt(), now).toHours());
            int targetLevel = hours < 12 ? 1 : 2 + (int) ((hours - 12) / 12);
            TaskEscalation state = escalations.findByTaskId(task.getId()).orElseGet(() -> new TaskEscalation(task));
            if (targetLevel <= state.getEscalationLevel()) continue;
            UserAccount recipient = recipient(task, targetLevel);
            List<UserAccount> recipients = recipient == null ? users.findEnabledByRole(Role.ADMIN) : List.of(recipient);
            notifications.notify(recipients, "TASK_OVERDUE", "任务逾期催办：" + task.getTitle(),
                "已逾期 " + hours + " 小时，升级级别 " + targetLevel, "TASK", task.getId(), targetLevel);
            state.advance(targetLevel, notifiedAt);
            escalations.save(state);
            count++;
        }
        return count;
    }

    private UserAccount recipient(TaskItem task, int level) {
        if (level == 1) return task.getOwner();
        if (level == 2) return task.getProject().getManager();
        if (level == 3) {
            Department department = task.getOwner().getDepartment();
            return department == null ? task.getProject().getManager() : department.getManager();
        }
        return null;
    }
}
