package com.fabbitinc.server.domain.activity.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ActivityTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    public Activity(
            ActivityTargetType targetType,
            UUID targetId,
            String action,
            UUID actorId,
            String detail
    ) {
        super(UuidV7Generator.next());
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.actorId = actorId;
        this.detail = detail;
    }
}
