package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.part.model.Part;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "project_parts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_project_parts_project_id_part_id",
                        columnNames = {"project_id", "part_id"}
                )
        },
        indexes = {
                @Index(name = "ix_project_parts_project_id", columnList = "project_id"),
                @Index(name = "ix_project_parts_part_id", columnList = "part_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectPart extends AbstractCreatedEntity {

    public static final String CODE_PROJECT_PART_PROJECT_REQUIRED = "PROJECT_PART_PROJECT_REQUIRED";
    public static final String CODE_PROJECT_PART_PART_REQUIRED = "PROJECT_PART_PART_REQUIRED";

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", insertable = false, updatable = false)
    private Part part;

    private ProjectPart(UUID projectId, UUID partId) {
        super(UuidV7Generator.next());
        this.projectId = requireProjectId(projectId);
        this.partId = requirePartId(partId);
    }

    public static ProjectPart link(UUID projectId, UUID partId) {
        return new ProjectPart(projectId, partId);
    }

    public static ProjectPart link(Project project, Part part) {
        if (project == null) {
            throw new DomainException(CODE_PROJECT_PART_PROJECT_REQUIRED, "프로젝트 ID는 필수입니다");
        }
        if (part == null) {
            throw new DomainException(CODE_PROJECT_PART_PART_REQUIRED, "부품 ID는 필수입니다");
        }
        ProjectPart projectPart = new ProjectPart(project.getId(), part.getId());
        projectPart.project = project;
        projectPart.part = part;
        return projectPart;
    }

    private UUID requireProjectId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_PROJECT_PART_PROJECT_REQUIRED, "프로젝트 ID는 필수입니다");
        }
        return value;
    }

    private UUID requirePartId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_PROJECT_PART_PART_REQUIRED, "부품 ID는 필수입니다");
        }
        return value;
    }
}
