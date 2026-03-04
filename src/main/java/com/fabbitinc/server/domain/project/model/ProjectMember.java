package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectRole role;

    public ProjectMember(UUID projectId, UUID userId, ProjectRole role) {
        super(UuidV7Generator.next());
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }
}
