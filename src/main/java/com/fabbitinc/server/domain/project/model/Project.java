package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.entity.AbstractSoftDeletableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends AbstractSoftDeletableEntity {

    public static final String CODE_PROJECT_NAME_REQUIRED = "PROJECT_NAME_REQUIRED";
    public static final String CODE_PROJECT_NAME_TOO_LONG = "PROJECT_NAME_TOO_LONG";
    public static final String CODE_PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String CODE_PROJECT_ALREADY_ARCHIVED = "PROJECT_ALREADY_ARCHIVED";
    public static final String CODE_PROJECT_NOT_ARCHIVED = "PROJECT_NOT_ARCHIVED";

    private static final int MAX_NAME_LENGTH = 200;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    private Project(String name, String description) {
        super(UuidV7Generator.next());
        this.name = validateName(name);
        this.description = description;
        this.archived = false;
    }

    public static Project create(String name, String description) {
        return new Project(name, description);
    }

    public void rename(String name) {
        ensureActive();
        this.name = validateName(name);
    }

    public void changeDescription(String description) {
        ensureActive();
        this.description = description;
    }

    public void archive() {
        if (archived) {
            throw new DomainException(CODE_PROJECT_ALREADY_ARCHIVED, "이미 보관된 프로젝트입니다");
        }
        this.archived = true;
    }

    public void unarchive() {
        if (!archived) {
            throw new DomainException(CODE_PROJECT_NOT_ARCHIVED, "보관 상태가 아닌 프로젝트는 복원할 수 없습니다");
        }
        this.archived = false;
    }

    public void ensureActive() {
        if (archived) {
            throw new DomainException(CODE_PROJECT_ARCHIVED, "보관된 프로젝트는 수정할 수 없습니다");
        }
    }

    private String validateName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new DomainException(CODE_PROJECT_NAME_REQUIRED, "프로젝트 이름은 필수입니다");
        }

        String trimmed = rawName.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_PROJECT_NAME_TOO_LONG, "프로젝트 이름은 200자 이하여야 합니다");
        }
        return trimmed;
    }
}
