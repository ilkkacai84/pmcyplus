package com.rcai.pm.project;

import com.rcai.pm.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberId.class)
public class ProjectMember {
    @Id
    @Column(name = "project_id")
    private Long projectId;
    @Id
    @Column(name = "user_id")
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserAccount user;
    @Column(name = "project_role", nullable = false)
    private String projectRole;

    protected ProjectMember() {}

    public ProjectMember(Long projectId, Long userId, String projectRole) {
        this.projectId = projectId;
        this.userId = userId;
        this.projectRole = projectRole;
    }

    public Long getProjectId() { return projectId; }
    public Long getUserId() { return userId; }
}
