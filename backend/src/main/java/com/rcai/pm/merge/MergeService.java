package com.rcai.pm.merge;

import com.rcai.pm.audit.AuditService;
import com.rcai.pm.common.ApiException;
import com.rcai.pm.project.Project;
import com.rcai.pm.project.ProjectRepository;
import com.rcai.pm.project.TaskItem;
import com.rcai.pm.project.TaskItemRepository;
import com.rcai.pm.requirement.Requirement;
import com.rcai.pm.requirement.RequirementRepository;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class MergeService {
    private final ProjectRepository projects;
    private final TaskItemRepository tasks;
    private final RequirementRepository requirements;
    private final MergeRecordRepository mergeRecords;
    private final UserAccountRepository users;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public MergeService(ProjectRepository projects, TaskItemRepository tasks,
                        RequirementRepository requirements, MergeRecordRepository mergeRecords,
                        UserAccountRepository users, AuditService audit, JdbcTemplate jdbc) {
        this.projects = projects;
        this.tasks = tasks;
        this.requirements = requirements;
        this.mergeRecords = mergeRecords;
        this.users = users;
        this.audit = audit;
        this.jdbc = jdbc;
    }

    public MergePreview preview(MergeObjectType type, List<Long> sourceIds, Long targetId) {
        Set<Long> uniqueSources = new LinkedHashSet<>(sourceIds);
        if (uniqueSources.isEmpty() || uniqueSources.contains(targetId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "来源对象不能为空且不能包含目标对象");
        }

        ObjectView target = view(type, targetId);
        List<ObjectView> sources = uniqueSources.stream().map(id -> view(type, id)).toList();
        List<FieldConflict> conflicts = new ArrayList<>();
        for (ObjectView source : sources) {
            target.fields().forEach((field, targetValue) -> {
                Object sourceValue = source.fields().get(field);
                if (sourceValue != null && !Objects.equals(sourceValue, targetValue)) {
                    conflicts.add(new FieldConflict(source.id(), field, sourceValue, targetValue));
                }
            });
        }

        Map<String, Long> migrationScope = new LinkedHashMap<>();
        scopeQueries(type).forEach((name, query) -> migrationScope.put(name,
            uniqueSources.stream().mapToLong(id -> count(query, id)).sum()));
        return new MergePreview(type, sources, target, migrationScope, conflicts,
            "来源对象将保留为只读已合并记录；执行前必须处理全部字段冲突");
    }

    @Transactional
    public MergeResult execute(MergeObjectType type, List<Long> sourceIds, Long targetId,
                               Set<String> acceptedTargetFields, Authentication authentication) {
        MergePreview preview = preview(type, sourceIds, targetId);
        Set<String> required = preview.conflicts().stream()
            .map(FieldConflict::field).collect(java.util.stream.Collectors.toSet());
        if (!acceptedTargetFields.containsAll(required)) {
            Set<String> missing = new LinkedHashSet<>(required);
            missing.removeAll(acceptedTargetFields);
            throw new ApiException(HttpStatus.CONFLICT, "仍有未确认的字段冲突: " + String.join(", ", missing));
        }

        for (ObjectView source : preview.sources()) {
            switch (type) {
                case PROJECT -> mergeProject(source.id(), targetId);
                case TASK -> mergeTask(source.id(), targetId);
                case REQUIREMENT -> mergeRequirement(source.id(), targetId);
            }
            jdbc.update("update notifications set object_id = ? where object_type = ? and object_id = ?",
                targetId, type.name(), source.id());
            moveNotificationWatchers(type.name(), source.id(), targetId);
        }

        UserAccount operator = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        String sourceJson = sourceIds.stream().distinct().map(String::valueOf)
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        String mapping = "{\"conflictPolicy\":\"KEEP_TARGET\",\"acceptedFields\":\""
            + String.join(",", acceptedTargetFields) + "\"}";
        MergeRecord record = mergeRecords.save(new MergeRecord(type, sourceJson, targetId, operator, mapping));
        audit.log(operator, "OBJECTS_MERGED", type.name(), targetId, Map.of(
            "sourceIds", sourceJson, "mergeRecordId", record.getId(), "conflictPolicy", "KEEP_TARGET"));
        return new MergeResult(record.getId(), type, sourceIds.stream().distinct().toList(), targetId,
            "COMPLETED", preview.migrationScope());
    }

    private void moveNotificationWatchers(String objectType, Long sourceId, Long targetId) {
        for (Long userId : jdbc.queryForList(
            "select user_id from notification_watchers where object_type = ? and object_id = ?",
            Long.class, objectType, sourceId)) {
            if (count("select count(*) from notification_watchers where object_type = ? and object_id = ? and user_id = ?",
                objectType, targetId, userId) == 0) {
                jdbc.update("insert into notification_watchers(object_type,object_id,user_id) values(?,?,?)",
                    objectType, targetId, userId);
            }
        }
        jdbc.update("delete from notification_watchers where object_type = ? and object_id = ?", objectType, sourceId);
    }

    private void mergeProject(Long sourceId, Long targetId) {
        Project source = projects.findById(sourceId).orElseThrow(() -> notFound(MergeObjectType.PROJECT, sourceId));
        ensureActive(source.getMergedIntoId());
        jdbc.update("update milestones set project_id = ? where project_id = ?", targetId, sourceId);
        jdbc.update("update task_items set project_id = ? where project_id = ?", targetId, sourceId);
        jdbc.update("update requirements set project_id = ? where project_id = ?", targetId, sourceId);
        jdbc.update("update risks set project_id = ? where project_id = ?", targetId, sourceId);
        jdbc.update("update documents set project_id = ? where project_id = ?", targetId, sourceId);
        mergeProjectMembers(sourceId, targetId);
        source.markMerged(targetId);
    }

    private void mergeTask(Long sourceId, Long targetId) {
        TaskItem source = tasks.findById(sourceId).orElseThrow(() -> notFound(MergeObjectType.TASK, sourceId));
        TaskItem target = tasks.findById(targetId).orElseThrow(() -> notFound(MergeObjectType.TASK, targetId));
        ensureActive(source.getMergedIntoId());
        jdbc.update("update task_items set parent_task_id = ?, project_id = ? where parent_task_id = ?",
            targetId, target.getProject().getId(), sourceId);
        mergeTaskParticipants(sourceId, targetId);
        jdbc.update("update worklogs set task_id = ? where task_id = ?", targetId, sourceId);
        moveDeliveries(sourceId, targetId);
        jdbc.update("update risks set task_id = ?, project_id = ? where task_id = ?",
            targetId, target.getProject().getId(), sourceId);
        mergeEscalation(sourceId, targetId);
        target.absorbActualHours(source.getActualHours());
        source.markMerged(targetId);
    }

    private void mergeRequirement(Long sourceId, Long targetId) {
        Requirement source = requirements.findById(sourceId)
            .orElseThrow(() -> notFound(MergeObjectType.REQUIREMENT, sourceId));
        ensureActive(source.getMergedIntoId());
        jdbc.update("update approval_instances set requirement_id = ? where requirement_id = ?", targetId, sourceId);
        source.markMerged(targetId);
    }

    private void mergeProjectMembers(Long sourceId, Long targetId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "select user_id, project_role from project_members where project_id = ?", sourceId);
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            if (count("select count(*) from project_members where project_id = ? and user_id = ?", targetId, userId) == 0) {
                jdbc.update("insert into project_members(project_id,user_id,project_role) values(?,?,?)",
                    targetId, userId, row.get("project_role"));
            }
        }
        jdbc.update("delete from project_members where project_id = ?", sourceId);
    }

    private void mergeTaskParticipants(Long sourceId, Long targetId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "select user_id, participant_type, planned_hours from task_participants where task_id = ?", sourceId);
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            BigDecimal hours = (BigDecimal) row.get("planned_hours");
            if (count("select count(*) from task_participants where task_id = ? and user_id = ?", targetId, userId) == 0) {
                jdbc.update("insert into task_participants(task_id,user_id,participant_type,planned_hours) values(?,?,?,?)",
                    targetId, userId, row.get("participant_type"), hours);
            } else {
                jdbc.update("update task_participants set planned_hours = planned_hours + ? where task_id = ? and user_id = ?",
                    hours, targetId, userId);
            }
        }
        jdbc.update("delete from task_participants where task_id = ?", sourceId);
    }

    private void moveDeliveries(Long sourceId, Long targetId) {
        Integer max = jdbc.queryForObject(
            "select coalesce(max(version_no),0) from delivery_versions where task_id = ?", Integer.class, targetId);
        int next = max == null ? 1 : max + 1;
        for (Long id : jdbc.queryForList(
            "select id from delivery_versions where task_id = ? order by version_no", Long.class, sourceId)) {
            jdbc.update("update delivery_versions set task_id = ?, version_no = ? where id = ?", targetId, next++, id);
        }
    }

    private void mergeEscalation(Long sourceId, Long targetId) {
        if (count("select count(*) from task_escalations where task_id = ?", sourceId) == 0) return;
        if (count("select count(*) from task_escalations where task_id = ?", targetId) == 0) {
            jdbc.update("update task_escalations set task_id = ? where task_id = ?", targetId, sourceId);
        } else {
            jdbc.update("delete from task_escalations where task_id = ?", sourceId);
        }
    }

    private void ensureActive(Long mergedIntoId) {
        if (mergedIntoId != null) throw new ApiException(HttpStatus.CONFLICT, "来源对象已经合并");
    }

    private ObjectView view(MergeObjectType type, Long id) {
        return switch (type) {
            case PROJECT -> {
                Project value = projects.findById(id).orElseThrow(() -> notFound(type, id));
                ensureActive(value.getMergedIntoId());
                yield projectView(value);
            }
            case TASK -> {
                TaskItem value = tasks.findById(id).orElseThrow(() -> notFound(type, id));
                ensureActive(value.getMergedIntoId());
                yield taskView(value);
            }
            case REQUIREMENT -> {
                Requirement value = requirements.findById(id).orElseThrow(() -> notFound(type, id));
                ensureActive(value.getMergedIntoId());
                yield requirementView(value);
            }
        };
    }

    private ObjectView projectView(Project project) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", project.getName());
        fields.put("description", project.getDescription());
        fields.put("projectType", project.getProjectType());
        fields.put("status", project.getStatus());
        fields.put("priority", project.getPriority());
        fields.put("managerId", project.getManager().getId());
        fields.put("customerId", project.getCustomer() == null ? null : project.getCustomer().getId());
        fields.put("plannedStartAt", project.getPlannedStartAt());
        fields.put("plannedEndAt", project.getPlannedEndAt());
        fields.put("budget", project.getBudget());
        fields.put("laborCost", project.getLaborCost());
        fields.put("otherCost", project.getOtherCost());
        return new ObjectView(project.getId(), project.getCode(), project.getName(), fields);
    }

    private ObjectView taskView(TaskItem task) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", task.getTitle());
        fields.put("description", task.getDescription());
        fields.put("projectId", task.getProject().getId());
        fields.put("milestoneId", task.getMilestone() == null ? null : task.getMilestone().getId());
        fields.put("parentTaskId", task.getParentTask() == null ? null : task.getParentTask().getId());
        fields.put("ownerId", task.getOwner().getId());
        fields.put("status", task.getStatus());
        fields.put("priority", task.getPriority());
        fields.put("plannedStartAt", task.getPlannedStartAt());
        fields.put("plannedEndAt", task.getPlannedEndAt());
        fields.put("estimatedHours", task.getEstimatedHours());
        return new ObjectView(task.getId(), "TASK-" + task.getId(), task.getTitle(), fields);
    }

    private ObjectView requirementView(Requirement requirement) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", requirement.getTitle());
        fields.put("description", requirement.getDescription());
        fields.put("source", requirement.getSource());
        fields.put("submitterId", requirement.getSubmitter().getId());
        fields.put("customerId", requirement.getCustomer() == null ? null : requirement.getCustomer().getId());
        fields.put("assigneeId", requirement.getAssignee() == null ? null : requirement.getAssignee().getId());
        fields.put("projectId", requirement.getProject() == null ? null : requirement.getProject().getId());
        fields.put("projectType", requirement.getProjectType());
        fields.put("status", requirement.getStatus());
        fields.put("priority", requirement.getPriority());
        return new ObjectView(requirement.getId(), requirement.getRequirementNo(), requirement.getTitle(), fields);
    }

    private Map<String, String> scopeQueries(MergeObjectType type) {
        return switch (type) {
            case PROJECT -> Map.of(
                "milestones", "select count(*) from milestones where project_id = ?",
                "tasks", "select count(*) from task_items where project_id = ?",
                "requirements", "select count(*) from requirements where project_id = ?",
                "members", "select count(*) from project_members where project_id = ?",
                "risks", "select count(*) from risks where project_id = ?",
                "documents", "select count(*) from documents where project_id = ?");
            case TASK -> Map.of(
                "subtasks", "select count(*) from task_items where parent_task_id = ?",
                "participants", "select count(*) from task_participants where task_id = ?",
                "worklogs", "select count(*) from worklogs where task_id = ?",
                "deliveries", "select count(*) from delivery_versions where task_id = ?",
                "risks", "select count(*) from risks where task_id = ?");
            case REQUIREMENT -> Map.of(
                "approvals", "select count(*) from approval_instances where requirement_id = ?");
        };
    }

    private long count(String query, Long id) {
        Long value = jdbc.queryForObject(query, Long.class, id);
        return value == null ? 0 : value;
    }

    private long count(String query, Object... args) {
        Long value = jdbc.queryForObject(query, Long.class, args);
        return value == null ? 0 : value;
    }

    private ApiException notFound(MergeObjectType type, Long id) {
        return new ApiException(HttpStatus.NOT_FOUND, type + " " + id + " 不存在");
    }

    public record MergePreview(MergeObjectType objectType, List<ObjectView> sources, ObjectView target,
                               Map<String, Long> migrationScope, List<FieldConflict> conflicts, String policy) {}
    public record ObjectView(Long id, String code, String title, Map<String, Object> fields) {}
    public record FieldConflict(Long sourceId, String field, Object sourceValue, Object targetValue) {}
    public record MergeResult(Long mergeRecordId, MergeObjectType objectType, List<Long> sourceIds,
                              Long targetId, String status, Map<String, Long> migratedScope) {}
}
