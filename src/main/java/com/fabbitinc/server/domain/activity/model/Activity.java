package com.fabbitinc.server.domain.activity.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "activities",
        indexes = {
                @Index(name = "ix_activities_target", columnList = "target_type,target_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity extends AbstractCreatedEntity {

    public static final String CODE_ACTIVITY_TARGET_TYPE_REQUIRED = "ACTIVITY_TARGET_TYPE_REQUIRED";
    public static final String CODE_ACTIVITY_TARGET_REQUIRED = "ACTIVITY_TARGET_REQUIRED";
    public static final String CODE_ACTIVITY_ACTION_REQUIRED = "ACTIVITY_ACTION_REQUIRED";
    public static final String CODE_ACTIVITY_ACTOR_REQUIRED = "ACTIVITY_ACTOR_REQUIRED";

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ActivityTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "actor_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_activities_actor_id")
    )
    private User _actorRelation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    private Activity(
            ActivityTargetType targetType,
            UUID targetId,
            String action,
            UUID actorId,
            String detail
    ) {
        super(UuidV7Generator.next());
        this.targetType = requireTargetType(targetType);
        this.targetId = requireTargetId(targetId);
        this.action = requireAction(action);
        this.actorId = requireActorId(actorId);
        this.detail = normalizeDetail(detail);
    }

    public static Activity create(
            ActivityTargetType targetType,
            UUID targetId,
            String action,
            UUID actorId,
            String detail
    ) {
        return new Activity(targetType, targetId, action, actorId, detail);
    }

    private ActivityTargetType requireTargetType(ActivityTargetType value) {
        if (value == null) {
            throw new DomainException(CODE_ACTIVITY_TARGET_TYPE_REQUIRED, "대상 타입은 필수입니다");
        }
        return value;
    }

    private UUID requireTargetId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ACTIVITY_TARGET_REQUIRED, "대상 ID는 필수입니다");
        }
        return value;
    }

    private String requireAction(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ACTIVITY_ACTION_REQUIRED, "액션은 필수입니다");
        }
        return value.trim();
    }

    private UUID requireActorId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ACTIVITY_ACTOR_REQUIRED, "행위자 ID는 필수입니다");
        }
        return value;
    }

    private String normalizeDetail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
