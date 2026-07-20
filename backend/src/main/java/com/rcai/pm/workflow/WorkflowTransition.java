package com.rcai.pm.workflow;

import com.rcai.pm.user.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workflow_transitions", uniqueConstraints = @UniqueConstraint(
    name = "uk_workflow_transition", columnNames = {"template_id", "object_type", "from_status", "to_status"}
))
public class WorkflowTransition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private WorkflowTemplate template;
    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false)
    private WorkflowObjectType objectType;
    @Column(name = "from_status", nullable = false, length = 40)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "requires_reason", nullable = false)
    private boolean requiresReason;
    @Column(name = "notification_event", length = 80)
    private String notificationEvent;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_transition_roles", joinColumns = @JoinColumn(name = "transition_id"))
    @Column(name = "role_code", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> allowedRoles = new HashSet<>();

    protected WorkflowTransition() {}

    public WorkflowTransition(WorkflowTemplate template, WorkflowObjectType objectType, String fromStatus,
                              String toStatus, Set<Role> allowedRoles, String notificationEvent) {
        this.template = template;
        this.objectType = objectType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.allowedRoles.addAll(allowedRoles);
        this.notificationEvent = notificationEvent;
    }

    public void configure(Set<Role> roles, boolean enabled, boolean requiresReason, String notificationEvent) {
        this.allowedRoles.clear();
        this.allowedRoles.addAll(roles);
        this.enabled = enabled;
        this.requiresReason = requiresReason;
        this.notificationEvent = notificationEvent;
    }

    public Long getId() { return id; }
    public WorkflowTemplate getTemplate() { return template; }
    public WorkflowObjectType getObjectType() { return objectType; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public boolean isEnabled() { return enabled; }
    public boolean isRequiresReason() { return requiresReason; }
    public String getNotificationEvent() { return notificationEvent; }
    public Set<Role> getAllowedRoles() { return Set.copyOf(allowedRoles); }
}
