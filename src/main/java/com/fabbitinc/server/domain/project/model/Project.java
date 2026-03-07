package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.entity.AbstractSoftDeletableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends AbstractSoftDeletableEntity implements AggregateRoot {

    public static final String CODE_PROJECT_NAME_REQUIRED = "PROJECT_NAME_REQUIRED";
    public static final String CODE_PROJECT_NAME_TOO_LONG = "PROJECT_NAME_TOO_LONG";
    public static final String CODE_PROJECT_ARCHIVED = "PROJECT_ARCHIVED";
    public static final String CODE_PROJECT_ALREADY_ARCHIVED = "PROJECT_ALREADY_ARCHIVED";
    public static final String CODE_PROJECT_NOT_ARCHIVED = "PROJECT_NOT_ARCHIVED";
    public static final String CODE_PROJECT_MEMBER_MISMATCH = "PROJECT_MEMBER_MISMATCH";

    private static final int MAX_NAME_LENGTH = 200;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<ProjectPart> parts = new ArrayList<>();

    private Project(String name, String description) {
        super(UuidV7Generator.next());
        this.name = validateName(name);
        this.description = normalizeDescription(description);
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
        this.description = normalizeDescription(description);
    }

    public ProjectMember addMember(UUID userId, ProjectRole role) {
        ensureActive();
        ProjectMember member = ProjectMember.assign(this, userId, role);
        members.add(member);
        return member;
    }

    public ProjectPart linkPart(UUID partId) {
        ensureActive();
        ProjectPart link = ProjectPart.link(this, partId);
        parts.add(link);
        return link;
    }

    public void changeMemberRole(ProjectMember member, ProjectRole role) {
        ensureActive();
        ProjectMember target = requireMember(member);
        if (!getId().equals(target.getProjectId())) {
            throw new DomainException(CODE_PROJECT_MEMBER_MISMATCH, "다른 프로젝트 멤버의 역할은 변경할 수 없습니다");
        }
        target.changeRole(role);
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

    public List<ProjectMember> getMembers() {
        return List.copyOf(members);
    }

    public List<ProjectPart> getParts() {
        return List.copyOf(parts);
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

    private String normalizeDescription(String rawDescription) {
        if (rawDescription == null) {
            return null;
        }
        String trimmed = rawDescription.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectMember requireMember(ProjectMember value) {
        if (value == null) {
            throw new DomainException(ProjectMember.CODE_PROJECT_MEMBER_PROJECT_REQUIRED, "프로젝트 멤버는 필수입니다");
        }
        return value;
    }
}
