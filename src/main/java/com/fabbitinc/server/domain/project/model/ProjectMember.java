package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "project_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_project_members_project_id_user_id",
                        columnNames = {"project_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "ix_project_members_project_id", columnList = "project_id"),
                @Index(name = "ix_project_members_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember extends AbstractCreatedEntity {

    public static final String CODE_PROJECT_MEMBER_PROJECT_REQUIRED = "PROJECT_MEMBER_PROJECT_REQUIRED";
    public static final String CODE_PROJECT_MEMBER_USER_REQUIRED = "PROJECT_MEMBER_USER_REQUIRED";
    public static final String CODE_PROJECT_MEMBER_ROLE_REQUIRED = "PROJECT_MEMBER_ROLE_REQUIRED";

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User _userRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectRole role;

    private ProjectMember(UUID projectId, UUID userId, ProjectRole role) {
        super(UuidV7Generator.next());
        this.projectId = requireProjectId(projectId);
        this.userId = requireUserId(userId);
        this.role = requireRole(role);
    }

    static ProjectMember assign(Project project, UUID userId, ProjectRole role) {
        if (project == null) {
            throw new DomainException(CODE_PROJECT_MEMBER_PROJECT_REQUIRED, "프로젝트 ID는 필수입니다");
        }
        if (userId == null) {
            throw new DomainException(CODE_PROJECT_MEMBER_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        ProjectMember member = new ProjectMember(project.getId(), userId, role);
        member.project = project;
        return member;
    }

    public boolean isAdmin() {
        return role == ProjectRole.ADMIN;
    }

    void changeRole(ProjectRole role) {
        this.role = requireRole(role);
    }

    private UUID requireProjectId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_PROJECT_MEMBER_PROJECT_REQUIRED, "프로젝트 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_PROJECT_MEMBER_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }

    private ProjectRole requireRole(ProjectRole value) {
        if (value == null) {
            throw new DomainException(CODE_PROJECT_MEMBER_ROLE_REQUIRED, "프로젝트 역할은 필수입니다");
        }
        return value;
    }
}
