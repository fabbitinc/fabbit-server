package com.fabbitinc.server.domain.workitem.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractComment extends AbstractActorAuditableEntity {

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    protected AbstractComment(String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.body = requireBody(body, bodyRequiredCode(), bodyRequiredMessage());
        initializeActor(requireActorId(actorId, actorRequiredCode(), actorRequiredMessage()));
    }

    public void updateBody(String body, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId, actorRequiredCode(), actorRequiredMessage());
        String requiredBody = requireBody(body, bodyRequiredCode(), bodyRequiredMessage());
        mutate(requiredActorId, () -> this.body = requiredBody);
    }

    public abstract UUID getTargetId();

    protected abstract String bodyRequiredCode();

    protected abstract String bodyRequiredMessage();

    protected abstract String actorRequiredCode();

    protected abstract String actorRequiredMessage();

    protected final UUID requireActorId(UUID value, String code, String message) {
        if (value == null) {
            throw new DomainException(code, message);
        }
        return value;
    }

    protected final String requireBody(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(code, message);
        }
        return value;
    }

}
