package com.fabbitinc.server.domain.label.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "labels",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_labels_name", columnNames = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Label extends AbstractAuditableEntity {

    public static final String CODE_LABEL_ACTOR_REQUIRED = "LABEL_ACTOR_REQUIRED";
    public static final String CODE_LABEL_NAME_REQUIRED = "LABEL_NAME_REQUIRED";
    public static final String CODE_LABEL_NAME_TOO_LONG = "LABEL_NAME_TOO_LONG";
    public static final String CODE_LABEL_DESCRIPTION_TOO_LONG = "LABEL_DESCRIPTION_TOO_LONG";
    public static final String CODE_LABEL_COLOR_REQUIRED = "LABEL_COLOR_REQUIRED";
    public static final String CODE_LABEL_COLOR_INVALID = "LABEL_COLOR_INVALID";

    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "created_by")
    private UUID createdBy;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User _createdByRelation;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    private User _updatedByRelation;

    private Label(String name, String description, String color, UUID actorId) {
        super(UuidV7Generator.next());
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.color = requireColor(color);
        UUID requiredActorId = requireActorId(actorId);
        this.createdBy = requiredActorId;
        this.updatedBy = requiredActorId;
    }

    public static Label create(String name, String description, String color, UUID actorId) {
        return new Label(name, description, color, actorId);
    }

    public void changeName(String name, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.name = requireName(name);
        touch(requiredActorId);
    }

    public void changeDescription(String description, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.description = normalizeDescription(description);
        touch(requiredActorId);
    }

    public void removeDescription(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.description = null;
        touch(requiredActorId);
    }

    public void changeColor(String color, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        this.color = requireColor(color);
        touch(requiredActorId);
    }

    private UUID requireActorId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return value;
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_LABEL_NAME_REQUIRED, "라벨 이름은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_LABEL_NAME_TOO_LONG, "라벨 이름은 50자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DomainException(CODE_LABEL_DESCRIPTION_TOO_LONG, "라벨 설명은 200자 이하여야 합니다");
        }
        return trimmed;
    }

    private String requireColor(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_LABEL_COLOR_REQUIRED, "라벨 색상은 필수입니다");
        }
        String trimmed = value.trim();
        if (!COLOR_PATTERN.matcher(trimmed).matches()) {
            throw new DomainException(CODE_LABEL_COLOR_INVALID, "라벨 색상은 #RRGGBB 형식이어야 합니다");
        }
        return trimmed;
    }

    private void touch(UUID actorId) {
        this.updatedBy = actorId;
        this._updatedByRelation = null;
    }
}
