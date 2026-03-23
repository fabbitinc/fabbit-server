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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "ix_chat_messages_thread_sequence", columnList = "thread_id,sequence"),
                @Index(name = "ix_chat_messages_run_id", columnList = "run_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends AbstractCreatedEntity {

    public static final String CODE_CHAT_MESSAGE_THREAD_REQUIRED = "CHAT_MESSAGE_THREAD_REQUIRED";
    public static final String CODE_CHAT_MESSAGE_ROLE_REQUIRED = "CHAT_MESSAGE_ROLE_REQUIRED";
    public static final String CODE_CHAT_MESSAGE_TYPE_REQUIRED = "CHAT_MESSAGE_TYPE_REQUIRED";
    public static final String CODE_CHAT_MESSAGE_SEQUENCE_INVALID = "CHAT_MESSAGE_SEQUENCE_INVALID";
    public static final String CODE_CHAT_MESSAGE_INVALID_STATE = "CHAT_MESSAGE_INVALID_STATE";

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "run_id")
    private UUID runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatMessageStatus status;

    @Column(name = "sequence", nullable = false)
    private long sequence;

    private ChatMessage(
            UUID threadId,
            UUID runId,
            ChatMessageRole role,
            ChatMessageType messageType,
            String content,
            ChatMessageStatus status,
            long sequence
    ) {
        super(UuidV7Generator.next());
        this.threadId = requireThreadId(threadId);
        this.runId = runId;
        this.role = requireRole(role);
        this.messageType = requireMessageType(messageType);
        this.content = normalizeContent(content);
        this.status = status;
        this.sequence = requireSequence(sequence);
    }

    public static ChatMessage createUserMessage(UUID threadId, String content, long sequence) {
        return new ChatMessage(
                threadId,
                null,
                ChatMessageRole.USER,
                ChatMessageType.TEXT,
                content,
                ChatMessageStatus.COMPLETED,
                sequence
        );
    }

    public static ChatMessage createAssistantMessage(UUID threadId, UUID runId, String content, long sequence) {
        return new ChatMessage(
                threadId,
                runId,
                ChatMessageRole.ASSISTANT,
                ChatMessageType.STRUCTURED,
                content,
                ChatMessageStatus.CREATED,
                sequence
        );
    }

    public static ChatMessage createAssistantNotice(UUID threadId, String content, long sequence) {
        return new ChatMessage(
                threadId,
                null,
                ChatMessageRole.ASSISTANT,
                ChatMessageType.STRUCTURED,
                content,
                ChatMessageStatus.COMPLETED,
                sequence
        );
    }

    public static ChatMessage createSystemNotice(UUID threadId, String content, long sequence) {
        return new ChatMessage(
                threadId,
                null,
                ChatMessageRole.SYSTEM,
                ChatMessageType.TEXT,
                content,
                ChatMessageStatus.COMPLETED,
                sequence
        );
    }

    public void startStreaming() {
        if (this.role != ChatMessageRole.ASSISTANT || this.status != ChatMessageStatus.CREATED) {
            throw new DomainException(CODE_CHAT_MESSAGE_INVALID_STATE, "ASSISTANT CREATED 상태에서만 스트리밍을 시작할 수 있습니다");
        }
        this.status = ChatMessageStatus.STREAMING;
    }

    public void complete(String content) {
        if (this.role != ChatMessageRole.ASSISTANT
                || (this.status != ChatMessageStatus.CREATED && this.status != ChatMessageStatus.STREAMING)) {
            throw new DomainException(CODE_CHAT_MESSAGE_INVALID_STATE, "스트리밍 중인 assistant 메시지만 완료할 수 있습니다");
        }
        this.content = normalizeContent(content);
        this.status = ChatMessageStatus.COMPLETED;
    }

    public void fail(String content) {
        if (this.role != ChatMessageRole.ASSISTANT) {
            throw new DomainException(CODE_CHAT_MESSAGE_INVALID_STATE, "assistant 메시지만 실패 처리할 수 있습니다");
        }
        this.content = normalizeContent(content);
        this.messageType = ChatMessageType.ERROR;
        this.status = ChatMessageStatus.FAILED;
    }

    private UUID requireThreadId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_MESSAGE_THREAD_REQUIRED, "스레드 ID는 필수입니다");
        }
        return value;
    }

    private ChatMessageRole requireRole(ChatMessageRole value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_MESSAGE_ROLE_REQUIRED, "메시지 역할은 필수입니다");
        }
        return value;
    }

    private ChatMessageType requireMessageType(ChatMessageType value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_MESSAGE_TYPE_REQUIRED, "메시지 타입은 필수입니다");
        }
        return value;
    }

    private String normalizeContent(String value) {
        if (value == null || value.isBlank()) {
            return "{\"text\":\"\"}";
        }
        return value.trim();
    }

    private long requireSequence(long value) {
        if (value <= 0) {
            throw new DomainException(CODE_CHAT_MESSAGE_SEQUENCE_INVALID, "메시지 순서는 1 이상이어야 합니다");
        }
        return value;
    }
}
