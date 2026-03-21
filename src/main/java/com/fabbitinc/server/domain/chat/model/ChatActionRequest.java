package com.fabbitinc.server.domain.chat.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
        name = "chat_action_requests",
        indexes = {
                @Index(name = "ix_chat_action_requests_thread_created_at", columnList = "thread_id,created_at"),
                @Index(name = "ix_chat_action_requests_status_expires_at", columnList = "status,expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatActionRequest extends AbstractAuditableEntity {

    public static final String CODE_CHAT_ACTION_RUN_REQUIRED = "CHAT_ACTION_RUN_REQUIRED";
    public static final String CODE_CHAT_ACTION_THREAD_REQUIRED = "CHAT_ACTION_THREAD_REQUIRED";
    public static final String CODE_CHAT_ACTION_TYPE_REQUIRED = "CHAT_ACTION_TYPE_REQUIRED";
    public static final String CODE_CHAT_ACTION_INVALID_STATE = "CHAT_ACTION_INVALID_STATE";
    public static final String CODE_CHAT_ACTION_CONFIRMED_BY_REQUIRED = "CHAT_ACTION_CONFIRMED_BY_REQUIRED";

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ChatActionRequestType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatActionRequestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preview_payload", nullable = false, columnDefinition = "jsonb")
    private String previewPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", nullable = false, columnDefinition = "jsonb")
    private String resultPayload;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    private ChatActionRequest(
            UUID runId,
            UUID threadId,
            ChatActionRequestType actionType,
            String previewPayload,
            String requestPayload,
            Instant expiresAt
    ) {
        super(UuidV7Generator.next());
        this.runId = requireRunId(runId);
        this.threadId = requireThreadId(threadId);
        this.actionType = requireActionType(actionType);
        this.status = ChatActionRequestStatus.PENDING;
        this.previewPayload = normalizeJson(previewPayload);
        this.requestPayload = normalizeJson(requestPayload);
        this.resultPayload = "{}";
        this.expiresAt = expiresAt;
    }

    public static ChatActionRequest create(
            UUID runId,
            UUID threadId,
            ChatActionRequestType actionType,
            String previewPayload,
            String requestPayload,
            Instant expiresAt
    ) {
        return new ChatActionRequest(runId, threadId, actionType, previewPayload, requestPayload, expiresAt);
    }

    public void confirm(UUID confirmedBy) {
        ensurePending();
        this.status = ChatActionRequestStatus.CONFIRMED;
        this.confirmedBy = requireConfirmedBy(confirmedBy);
        this.confirmedAt = Instant.now();
    }

    public void reject() {
        ensurePending();
        this.status = ChatActionRequestStatus.REJECTED;
    }

    public void execute(String resultPayload) {
        if (this.status != ChatActionRequestStatus.CONFIRMED) {
            throw new DomainException(CODE_CHAT_ACTION_INVALID_STATE, "확인된 액션만 실행 완료 처리할 수 있습니다");
        }
        this.status = ChatActionRequestStatus.EXECUTED;
        this.resultPayload = normalizeJson(resultPayload);
        this.executedAt = Instant.now();
    }

    public void fail(String resultPayload) {
        if (this.status != ChatActionRequestStatus.CONFIRMED && this.status != ChatActionRequestStatus.PENDING) {
            throw new DomainException(CODE_CHAT_ACTION_INVALID_STATE, "대기 또는 확인 상태에서만 실패 처리할 수 있습니다");
        }
        this.status = ChatActionRequestStatus.FAILED;
        this.resultPayload = normalizeJson(resultPayload);
    }

    public void expire() {
        ensurePending();
        this.status = ChatActionRequestStatus.EXPIRED;
    }

    public void ensurePending() {
        if (this.status != ChatActionRequestStatus.PENDING) {
            throw new DomainException(CODE_CHAT_ACTION_INVALID_STATE, "대기 중인 액션 요청만 변경할 수 있습니다");
        }
    }

    private UUID requireRunId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_ACTION_RUN_REQUIRED, "run ID는 필수입니다");
        }
        return value;
    }

    private UUID requireThreadId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_ACTION_THREAD_REQUIRED, "thread ID는 필수입니다");
        }
        return value;
    }

    private ChatActionRequestType requireActionType(ChatActionRequestType value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_ACTION_TYPE_REQUIRED, "액션 타입은 필수입니다");
        }
        return value;
    }

    private UUID requireConfirmedBy(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_ACTION_CONFIRMED_BY_REQUIRED, "확인 사용자 ID는 필수입니다");
        }
        return value;
    }

    private String normalizeJson(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }
}
