package com.rcai.pm.common;

import com.rcai.pm.project.TaskItemRepository;
import com.rcai.pm.project.TaskStatus;
import com.rcai.pm.requirement.RequirementRepository;
import com.rcai.pm.requirement.RequirementStatus;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import com.rcai.pm.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final TaskItemRepository tasks;
    private final RequirementRepository requirements;
    private final UserAccountRepository users;

    public DashboardController(TaskItemRepository tasks, RequirementRepository requirements, UserAccountRepository users) {
        this.tasks = tasks;
        this.requirements = requirements;
        this.users = users;
    }

    @GetMapping
    public Dashboard dashboard(Authentication authentication) {
        UserAccount user = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        List<TaskStatus> closed = List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.MERGED);
        boolean canManageRequirements = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.PROJECT_MANAGER);
        return new Dashboard(
            tasks.countByOwnerIdAndStatusNotIn(user.getId(), closed),
            tasks.countByOwnerIdAndPlannedEndAtBeforeAndStatusNotIn(user.getId(), LocalDateTime.now(), closed),
            canManageRequirements ? requirements.countByStatus(RequirementStatus.UNASSIGNED) : 0
        );
    }

    public record Dashboard(long openTasks, long overdueTasks, long unassignedRequirements) {}
}
