package com.rcai.pm.project;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectService.ProjectSummary> list(Authentication authentication) {
        return service.list(authentication);
    }

    @GetMapping("/{projectId}")
    public ProjectService.ProjectDetails get(@PathVariable Long projectId, Authentication authentication) {
        return service.get(projectId, authentication);
    }

    @PostMapping
    public ProjectService.ProjectSummary create(@Valid @RequestBody ProjectService.CreateProject request, Authentication authentication) {
        return service.create(request, authentication);
    }

    @PatchMapping("/{projectId}/status")
    public ProjectService.ProjectSummary changeStatus(@PathVariable Long projectId, @RequestParam ProjectStatus status, Authentication authentication) {
        return service.changeProjectStatus(projectId, status, authentication);
    }

    @PostMapping("/{projectId}/milestones")
    public ProjectService.MilestoneView createMilestone(@PathVariable Long projectId,
                                                         @Valid @RequestBody ProjectService.CreateMilestone request,
                                                         Authentication authentication) {
        return service.createMilestone(projectId, request, authentication);
    }

    @PostMapping("/{projectId}/tasks")
    public ProjectService.TaskView createTask(@PathVariable Long projectId,
                                               @Valid @RequestBody ProjectService.CreateTask request,
                                               Authentication authentication) {
        return service.createTask(projectId, request, authentication);
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ProjectService.TaskView transitionTask(@PathVariable Long taskId, @RequestParam TaskStatus status, Authentication authentication) {
        return service.transitionTask(taskId, status, authentication);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ProjectService.TaskView complete(@PathVariable Long taskId,
                                             @Valid @RequestBody ProjectService.CompleteTask request,
                                             Authentication authentication) {
        return service.completeWithWorklog(taskId, request, authentication);
    }

    @PostMapping("/tasks/{taskId}/review")
    public ProjectService.DeliveryView review(@PathVariable Long taskId,
                                               @Valid @RequestBody ProjectService.ReviewDelivery request,
                                               Authentication authentication) {
        return service.reviewDelivery(taskId, request, authentication);
    }
}
