package com.fabbitinc.server.domain.label.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public Label(String name, String description, String color, UUID actorId) {
        super(UuidV7Generator.next());
        this.name = name;
        this.description = description;
        this.color = color;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void changeName(String name, UUID actorId) {
        this.name = name;
        this.updatedBy = actorId;
    }

    public void changeDescription(String description, UUID actorId) {
        this.description = description;
        this.updatedBy = actorId;
    }

    public void removeDescription(UUID actorId) {
        this.description = null;
        this.updatedBy = actorId;
    }

    public void changeColor(String color, UUID actorId) {
        this.color = color;
        this.updatedBy = actorId;
    }
}
