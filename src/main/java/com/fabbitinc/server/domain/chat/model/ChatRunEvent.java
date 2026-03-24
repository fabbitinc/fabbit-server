package com.fabbitinc.server.domain.chat.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
        name = "chat_run_events",
        indexes = {
                @Index(name = "ix_chat_run_events_run_sequence", columnList = "run_id,sequence")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRunEvent extends AbstractCreatedEntity {

    public static final String CODE_CHAT_RUN_EVENT_RUN_REQUIRED = "CHAT_RUN_EVENT_RUN_REQUIRED";
    public static final String CODE_CHAT_RUN_EVENT_TYPE_REQUIRED = "CHAT_RUN_EVENT_TYPE_REQUIRED";
    public static final String CODE_CHAT_RUN_EVENT_SEQUENCE_INVALID = "CHAT_RUN_EVENT_SEQUENCE_INVALID";
    public static final String CODE_CHAT_RUN_EVENT_VISIBILITY_REQUIRED = "CHAT_RUN_EVENT_VISIBILITY_REQUIRED";

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "sequence", nullable = false)
    private long sequence;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private ChatRunEventVisibility visibility;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    private ChatRunEvent(
            UUID runId,
            long sequence,
            String eventType,
            ChatRunEventVisibility visibility,
            String payload
    ) {
        super(UuidV7Generator.next());
        this.runId = requireRunId(runId);
        this.sequence = requireSequence(sequence);
        this.eventType = requireEventType(eventType);
        this.visibility = requireVisibility(visibility);
        this.payload = normalizePayload(payload);
    }

    public static ChatRunEvent create(
            UUID runId,
            long sequence,
            String eventType,
            ChatRunEventVisibility visibility,
            String payload
    ) {
        return new ChatRunEvent(runId, sequence, eventType, visibility, payload);
    }

    private UUID requireRunId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_RUN_EVENT_RUN_REQUIRED, "run ID는 필수입니다");
        }
        return value;
    }

    private long requireSequence(long value) {
        if (value <= 0) {
            throw new DomainException(CODE_CHAT_RUN_EVENT_SEQUENCE_INVALID, "이벤트 순서는 1 이상이어야 합니다");
        }
        return value;
    }

    private String requireEventType(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_CHAT_RUN_EVENT_TYPE_REQUIRED, "이벤트 타입은 필수입니다");
        }
        return value.trim();
    }

    private ChatRunEventVisibility requireVisibility(ChatRunEventVisibility value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_RUN_EVENT_VISIBILITY_REQUIRED, "이벤트 노출 수준은 필수입니다");
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
