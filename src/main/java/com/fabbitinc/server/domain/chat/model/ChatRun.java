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
        name = "chat_runs",
        indexes = {
                @Index(name = "ix_chat_runs_thread_created_at", columnList = "thread_id,created_at"),
                @Index(name = "ix_chat_runs_status_created_at", columnList = "status,created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRun extends AbstractAuditableEntity {

    public static final String CODE_CHAT_RUN_THREAD_REQUIRED = "CHAT_RUN_THREAD_REQUIRED";
    public static final String CODE_CHAT_RUN_USER_MESSAGE_REQUIRED = "CHAT_RUN_USER_MESSAGE_REQUIRED";
    public static final String CODE_CHAT_RUN_MODEL_REQUIRED = "CHAT_RUN_MODEL_REQUIRED";
    public static final String CODE_CHAT_RUN_INTENT_REQUIRED = "CHAT_RUN_INTENT_REQUIRED";
    public static final String CODE_CHAT_RUN_INVALID_STATE = "CHAT_RUN_INVALID_STATE";
    public static final String CODE_CHAT_RUN_ASSISTANT_MESSAGE_REQUIRED = "CHAT_RUN_ASSISTANT_MESSAGE_REQUIRED";

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "user_message_id", nullable = false)
    private UUID userMessageId;

    @Column(name = "assistant_message_id")
    private UUID assistantMessageId;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "intent", nullable = false, length = 50)
    private ChatIntent intent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChatRunStatus status;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private ChatRun(
            UUID threadId,
            UUID userMessageId,
            String model,
            ChatIntent intent,
            String metadata
    ) {
        super(UuidV7Generator.next());
        this.threadId = requireThreadId(threadId);
        this.userMessageId = requireUserMessageId(userMessageId);
        this.model = requireModel(model);
        this.intent = requireIntent(intent);
        this.status = ChatRunStatus.QUEUED;
        this.inputTokens = 0;
        this.outputTokens = 0;
        this.metadata = normalizeMetadata(metadata);
    }

    public static ChatRun create(
            UUID threadId,
            UUID userMessageId,
            String model,
            ChatIntent intent,
            String metadata
    ) {
        return new ChatRun(threadId, userMessageId, model, intent, metadata);
    }

    public void attachAssistantMessage(UUID assistantMessageId) {
        if (assistantMessageId == null) {
            throw new DomainException(CODE_CHAT_RUN_ASSISTANT_MESSAGE_REQUIRED, "assistant 메시지 ID는 필수입니다");
        }
        this.assistantMessageId = assistantMessageId;
    }

    public void start() {
        if (this.status != ChatRunStatus.QUEUED) {
            throw new DomainException(CODE_CHAT_RUN_INVALID_STATE, "QUEUED 상태에서만 실행을 시작할 수 있습니다");
        }
        this.status = ChatRunStatus.RUNNING;
        this.startedAt = Instant.now();
        this.completedAt = null;
        this.errorCode = null;
    }

    public void waitForConfirmation(String metadata) {
        if (this.status != ChatRunStatus.RUNNING) {
            throw new DomainException(CODE_CHAT_RUN_INVALID_STATE, "RUNNING 상태에서만 확인 대기로 전환할 수 있습니다");
        }
        this.status = ChatRunStatus.WAITING_CONFIRMATION;
        this.metadata = normalizeMetadata(metadata);
    }

    public void complete(int inputTokens, int outputTokens, String metadata) {
        if (this.status != ChatRunStatus.RUNNING && this.status != ChatRunStatus.WAITING_CONFIRMATION) {
            throw new DomainException(CODE_CHAT_RUN_INVALID_STATE, "실행 중인 run만 완료할 수 있습니다");
        }
        this.inputTokens = Math.max(inputTokens, 0);
        this.outputTokens = Math.max(outputTokens, 0);
        this.metadata = normalizeMetadata(metadata);
        this.status = ChatRunStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.errorCode = null;
    }

    public void fail(String errorCode, String metadata) {
        if (this.status == ChatRunStatus.COMPLETED || this.status == ChatRunStatus.CANCELLED) {
            throw new DomainException(CODE_CHAT_RUN_INVALID_STATE, "완료되거나 취소된 run은 실패 처리할 수 없습니다");
        }
        this.errorCode = normalizeErrorCode(errorCode);
        this.metadata = normalizeMetadata(metadata);
        this.status = ChatRunStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == ChatRunStatus.COMPLETED || this.status == ChatRunStatus.FAILED) {
            throw new DomainException(CODE_CHAT_RUN_INVALID_STATE, "종료된 run은 취소할 수 없습니다");
        }
        this.status = ChatRunStatus.CANCELLED;
        this.completedAt = Instant.now();
    }

    private UUID requireThreadId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_RUN_THREAD_REQUIRED, "스레드 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserMessageId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_RUN_USER_MESSAGE_REQUIRED, "사용자 메시지 ID는 필수입니다");
        }
        return value;
    }

    private String requireModel(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_CHAT_RUN_MODEL_REQUIRED, "모델명은 필수입니다");
        }
        return value.trim();
    }

    private ChatIntent requireIntent(ChatIntent value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_RUN_INTENT_REQUIRED, "의도 값은 필수입니다");
        }
        return value;
    }

    private String normalizeMetadata(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }

    private String normalizeErrorCode(String value) {
        if (value == null || value.isBlank()) {
            return "CHAT_RUN_FAILED";
        }
        return value.trim();
    }
}
