package com.rcai.pm.user;

import com.rcai.pm.common.ApiException;
import com.rcai.pm.project.TaskItem;
import com.rcai.pm.project.TaskItemRepository;
import com.rcai.pm.project.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DepartmentService {
    private final DepartmentRepository departments;
    private final UserAccountRepository users;
    private final TaskItemRepository tasks;

    public DepartmentService(DepartmentRepository departments, UserAccountRepository users, TaskItemRepository tasks) {
        this.departments = departments;
        this.users = users;
        this.tasks = tasks;
    }

    public List<DepartmentView> list() {
        return departments.findAllByOrderByNameAsc().stream().map(DepartmentView::from).toList();
    }

    public DepartmentScope myScope(Authentication authentication) {
        UserAccount actor = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        if (actor.getDepartmentId() == null) {
            return new DepartmentScope(null, List.of());
        }
        Department department = actor.getDepartment();
        List<DepartmentTaskView> taskViews = tasks
            .findDepartmentTasks(department.getId())
            .stream().map(DepartmentTaskView::from).toList();
        return new DepartmentScope(DepartmentView.from(department), taskViews);
    }

    @Transactional
    public DepartmentView create(SaveDepartment request) {
        String name = request.name().trim();
        if (departments.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "部门名称已存在");
        }
        Department department = new Department(name, parent(request.parentId(), null), manager(request.managerId()));
        return DepartmentView.from(departments.save(department));
    }

    @Transactional
    public DepartmentView update(Long id, SaveDepartment request) {
        Department department = departments.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "部门不存在"));
        String name = request.name().trim();
        if (!department.getName().equalsIgnoreCase(name) && departments.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "部门名称已存在");
        }
        department.update(name, parent(request.parentId(), id), manager(request.managerId()));
        return DepartmentView.from(department);
    }

    private Department parent(Long parentId, Long departmentId) {
        if (parentId == null) return null;
        if (parentId.equals(departmentId)) throw new ApiException(HttpStatus.BAD_REQUEST, "部门不能作为自己的上级");
        return departments.findById(parentId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "上级部门不存在"));
    }

    private UserAccount manager(Long managerId) {
        if (managerId == null) return null;
        UserAccount manager = users.findById(managerId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "部门负责人不存在"));
        if (manager.getUserType() != UserType.INTERNAL || !manager.getRoles().contains(Role.DEPARTMENT_MANAGER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "部门负责人必须是具备部门负责人角色的内部用户");
        }
        return manager;
    }

    public record SaveDepartment(@NotBlank String name, Long parentId, Long managerId) {}
    public record DepartmentView(Long id, String name, Long parentId, Long managerId, String managerName) {
        static DepartmentView from(Department department) {
            UserAccount manager = department.getManager();
            return new DepartmentView(department.getId(), department.getName(),
                department.getParent() == null ? null : department.getParent().getId(),
                manager == null ? null : manager.getId(), manager == null ? null : manager.getDisplayName());
        }
    }
    public record DepartmentScope(DepartmentView department, List<DepartmentTaskView> tasks) {}
    public record DepartmentTaskView(Long id, Long projectId, String projectName, String title, Long ownerId,
                                     String ownerName, TaskStatus status, java.time.LocalDateTime plannedEndAt) {
        static DepartmentTaskView from(TaskItem task) {
            return new DepartmentTaskView(task.getId(), task.getProject().getId(), task.getProject().getName(),
                task.getTitle(), task.getOwner().getId(), task.getOwner().getDisplayName(), task.getStatus(),
                task.getPlannedEndAt());
        }
    }
}
