package com.fabbitinc.server.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractSoftDeletableEntity extends AbstractActorAuditableEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    protected AbstractSoftDeletableEntity(UUID id) {
        super(id);
        this.deleted = false;
    }

    public void softDelete(String actor) {
        if (deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = actor;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
