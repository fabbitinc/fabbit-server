package com.fabbitinc.server.domain.part.model;

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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "part_revision_histories",
        indexes = {
                @Index(name = "ix_part_revision_histories_revision_created", columnList = "part_revision_id,created_at"),
                @Index(name = "ix_part_revision_histories_actor_id", columnList = "actor_id"),
                @Index(name = "ix_part_revision_histories_source_ref_id", columnList = "source_ref_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartRevisionHistory extends AbstractCreatedEntity {

    public static final String CODE_PART_REVISION_HISTORY_REVISION_REQUIRED = "PART_REVISION_HISTORY_REVISION_REQUIRED";
    public static final String CODE_PART_REVISION_HISTORY_ACTION_REQUIRED = "PART_REVISION_HISTORY_ACTION_REQUIRED";
    public static final String CODE_PART_REVISION_HISTORY_SOURCE_REQUIRED = "PART_REVISION_HISTORY_SOURCE_REQUIRED";
    public static final String CODE_PART_REVISION_HISTORY_OCCURRED_AT_REQUIRED = "PART_REVISION_HISTORY_OCCURRED_AT_REQUIRED";

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "part_revision_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_part_revision_histories_part_revision_id")
    )
    private PartRevision partRevision;

    @Column(name = "part_revision_id", nullable = false, insertable = false, updatable = false)
    private UUID partRevisionId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "actor_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_part_revision_histories_actor_id")
    )
    private User _actorRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private PartRevisionHistoryActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private PartRevisionHistorySourceType sourceType;

    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private PartRevisionHistory(
            PartRevision partRevision,
            UUID actorId,
            PartRevisionHistoryActionType actionType,
            PartRevisionHistorySourceType sourceType,
            UUID sourceRefId,
            String payload,
            Instant occurredAt
    ) {
        super(UuidV7Generator.next());
        PartRevision requiredPartRevision = requirePartRevision(partRevision);
        this.partRevision = requiredPartRevision;
        this.partRevisionId = requiredPartRevision.getId();
        this.actorId = actorId;
        this.actionType = requireActionType(actionType);
        this.sourceType = requireSourceType(sourceType);
        this.sourceRefId = sourceRefId;
        this.payload = normalizePayload(payload);
        this.occurredAt = requireOccurredAt(occurredAt);
    }

    static PartRevisionHistory record(
            PartRevision partRevision,
            UUID actorId,
            PartRevisionHistoryActionType actionType,
            PartRevisionHistorySourceType sourceType,
            UUID sourceRefId,
            String payload
    ) {
        return new PartRevisionHistory(
                partRevision,
                actorId,
                actionType,
                sourceType,
                sourceRefId,
                payload,
                Instant.now()
        );
    }

    static PartRevisionHistory recordAt(
            PartRevision partRevision,
            UUID actorId,
            PartRevisionHistoryActionType actionType,
            PartRevisionHistorySourceType sourceType,
            UUID sourceRefId,
            String payload,
            Instant occurredAt
    ) {
        return new PartRevisionHistory(
                partRevision,
                actorId,
                actionType,
                sourceType,
                sourceRefId,
                payload,
                occurredAt
        );
    }

    private PartRevision requirePartRevision(PartRevision value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_HISTORY_REVISION_REQUIRED, "리비전은 필수입니다");
        }
        return value;
    }

    private PartRevisionHistoryActionType requireActionType(PartRevisionHistoryActionType value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_HISTORY_ACTION_REQUIRED, "이력 타입은 필수입니다");
        }
        return value;
    }

    private PartRevisionHistorySourceType requireSourceType(PartRevisionHistorySourceType value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_HISTORY_SOURCE_REQUIRED, "이력 출처 타입은 필수입니다");
        }
        return value;
    }

    private Instant requireOccurredAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_HISTORY_OCCURRED_AT_REQUIRED, "발생 시각은 필수입니다");
        }
        return value;
    }

    private String normalizePayload(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }
}
