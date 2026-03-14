package com.fabbitinc.server.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractActorAuditableEntity extends AbstractAuditableEntity {

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AbstractActorAuditableEntity(UUID id) {
        super(id);
    }

    protected final void initializeActor(UUID actorId) {
        if (actorId == null) {
            return;
        }
        boolean createdByInitialized = this.createdBy == null;
        this.createdBy = actorId;
        this.updatedBy = actorId;
        afterActorTouched(actorId, createdByInitialized);
    }

    protected final void mutate(UUID actorId, Runnable change) {
        Objects.requireNonNull(change, "change");
        change.run();
        touch(actorId);
    }

    protected final void touch(UUID actorId) {
        if (actorId == null) {
            return;
        }
        boolean createdByInitialized = false;
        if (this.createdBy == null) {
            this.createdBy = actorId;
            createdByInitialized = true;
        }
        this.updatedBy = actorId;
        afterActorTouched(actorId, createdByInitialized);
    }

    protected void afterActorTouched(UUID actorId, boolean createdByInitialized) {
    }
}
