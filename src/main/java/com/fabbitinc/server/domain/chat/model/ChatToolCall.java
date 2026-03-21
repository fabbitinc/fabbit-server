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
        name = "chat_tool_calls",
        indexes = {
                @Index(name = "ix_chat_tool_calls_run_created_at", columnList = "run_id,created_at"),
                @Index(name = "ix_chat_tool_calls_tool_status_created_at", columnList = "tool_name,status,created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatToolCall extends AbstractAuditableEntity {

    public static final String CODE_CHAT_TOOL_CALL_RUN_REQUIRED = "CHAT_TOOL_CALL_RUN_REQUIRED";
    public static final String CODE_CHAT_TOOL_CALL_THREAD_REQUIRED = "CHAT_TOOL_CALL_THREAD_REQUIRED";
    public static final String CODE_CHAT_TOOL_CALL_NAME_REQUIRED = "CHAT_TOOL_CALL_NAME_REQUIRED";
    public static final String CODE_CHAT_TOOL_CALL_INVALID_STATE = "CHAT_TOOL_CALL_INVALID_STATE";

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatToolCallStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "arguments_json", nullable = false, columnDefinition = "jsonb")
    private String argumentsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private String resultJson;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private ChatToolCall(
            UUID runId,
            UUID threadId,
            String toolName,
            String argumentsJson
    ) {
        super(UuidV7Generator.next());
        this.runId = requireRunId(runId);
        this.threadId = requireThreadId(threadId);
        this.toolName = requireToolName(toolName);
        this.status = ChatToolCallStatus.STARTED;
        this.argumentsJson = normalizeJson(argumentsJson);
        this.resultJson = "{}";
        this.startedAt = Instant.now();
    }

    public static ChatToolCall create(
            UUID runId,
            UUID threadId,
            String toolName,
            String argumentsJson
    ) {
        return new ChatToolCall(runId, threadId, toolName, argumentsJson);
    }

    public void complete(String resultJson) {
        if (this.status != ChatToolCallStatus.STARTED) {
            throw new DomainException(CODE_CHAT_TOOL_CALL_INVALID_STATE, "시작된 tool call만 완료할 수 있습니다");
        }
        this.status = ChatToolCallStatus.COMPLETED;
        this.resultJson = normalizeJson(resultJson);
        this.errorCode = null;
        this.completedAt = Instant.now();
    }

    public void fail(String errorCode, String resultJson) {
        if (this.status != ChatToolCallStatus.STARTED) {
            throw new DomainException(CODE_CHAT_TOOL_CALL_INVALID_STATE, "시작된 tool call만 실패 처리할 수 있습니다");
        }
        this.status = ChatToolCallStatus.FAILED;
        this.errorCode = normalizeErrorCode(errorCode);
        this.resultJson = normalizeJson(resultJson);
        this.completedAt = Instant.now();
    }

    private UUID requireRunId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_TOOL_CALL_RUN_REQUIRED, "run ID는 필수입니다");
        }
        return value;
    }

    private UUID requireThreadId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_TOOL_CALL_THREAD_REQUIRED, "thread ID는 필수입니다");
        }
        return value;
    }

    private String requireToolName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_CHAT_TOOL_CALL_NAME_REQUIRED, "tool 이름은 필수입니다");
        }
        return value.trim();
    }

    private String normalizeJson(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }

    private String normalizeErrorCode(String value) {
        if (value == null || value.isBlank()) {
            return "CHAT_TOOL_CALL_FAILED";
        }
        return value.trim();
    }
}
