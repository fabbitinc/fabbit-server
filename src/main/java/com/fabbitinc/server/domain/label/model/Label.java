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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "created_by")
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User createdByUser;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    private User updatedByUser;

    public Label(String name, String description, String color, UUID actorId) {
        super(UuidV7Generator.next());
        this.name = name;
        this.description = description;
        this.color = color;
        UUID requiredActorId = requireActorId(actorId);
        this.createdBy = requiredActorId;
        this.updatedBy = requiredActorId;
    }

    public static Label create(String name, String description, String color, UUID actorId) {
        return new Label(name, description, color, actorId);
    }

    public static Label create(String name, String description, String color, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        Label label = new Label(name, description, color, actor.getId());
        label.createdByUser = actor;
        label.updatedByUser = actor;
        return label;
    }

    public void changeName(String name, UUID actorId) {
        this.name = name;
        this.updatedBy = requireActorId(actorId);
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void changeName(String name, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.name = name;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    public void changeDescription(String description, UUID actorId) {
        this.description = description;
        this.updatedBy = requireActorId(actorId);
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void changeDescription(String description, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.description = description;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    public void removeDescription(UUID actorId) {
        this.description = null;
        this.updatedBy = requireActorId(actorId);
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void removeDescription(User actor) {
        if (actor == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.description = null;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    public void changeColor(String color, UUID actorId) {
        this.color = color;
        this.updatedBy = requireActorId(actorId);
        if (updatedByUser != null && !this.updatedBy.equals(updatedByUser.getId())) {
            this.updatedByUser = null;
        }
    }

    public void changeColor(String color, User actor) {
        if (actor == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        this.color = color;
        this.updatedBy = actor.getId();
        this.updatedByUser = actor;
    }

    private UUID requireActorId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_LABEL_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return value;
    }
}
