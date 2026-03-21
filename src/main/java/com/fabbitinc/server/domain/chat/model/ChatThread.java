package com.fabbitinc.server.domain.chat.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
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

@Getter
@Entity
@Table(
        name = "chat_threads",
        indexes = {
                @Index(name = "ix_chat_threads_org_user_created_at", columnList = "org_id,user_id,created_at"),
                @Index(name = "ix_chat_threads_org_context", columnList = "org_id,context_type,context_id"),
                @Index(name = "ix_chat_threads_org_last_message_at", columnList = "org_id,last_message_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatThread extends AbstractAuditableEntity implements AggregateRoot {

    public static final String CODE_CHAT_THREAD_ORG_REQUIRED = "CHAT_THREAD_ORG_REQUIRED";
    public static final String CODE_CHAT_THREAD_USER_REQUIRED = "CHAT_THREAD_USER_REQUIRED";
    public static final String CODE_CHAT_THREAD_CONTEXT_TYPE_REQUIRED = "CHAT_THREAD_CONTEXT_TYPE_REQUIRED";
    public static final String CODE_CHAT_THREAD_TITLE_REQUIRED = "CHAT_THREAD_TITLE_REQUIRED";
    public static final String CODE_CHAT_THREAD_LAST_MESSAGE_AT_REQUIRED = "CHAT_THREAD_LAST_MESSAGE_AT_REQUIRED";
    public static final String CODE_CHAT_THREAD_INVALID_STATE = "CHAT_THREAD_INVALID_STATE";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "context_type", nullable = false, length = 30)
    private String contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatThreadStatus status;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    private ChatThread(
            UUID orgId,
            UUID userId,
            UUID projectId,
            String contextType,
            UUID contextId,
            String title
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.userId = requireUserId(userId);
        this.projectId = projectId;
        this.contextType = requireContextType(contextType);
        this.contextId = contextId;
        this.title = requireTitle(title);
        this.status = ChatThreadStatus.ACTIVE;
        this.lastMessageAt = Instant.now();
    }

    public static ChatThread create(
            UUID orgId,
            UUID userId,
            UUID projectId,
            String contextType,
            UUID contextId,
            String title
    ) {
        return new ChatThread(orgId, userId, projectId, contextType, contextId, title);
    }

    public void updateTitle(String title) {
        this.title = requireTitle(title);
    }

    public void touchLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = requireLastMessageAt(lastMessageAt);
    }

    public void archive() {
        if (this.status == ChatThreadStatus.ARCHIVED) {
            return;
        }
        this.status = ChatThreadStatus.ARCHIVED;
    }

    public void ensureActive() {
        if (this.status != ChatThreadStatus.ACTIVE) {
            throw new DomainException(CODE_CHAT_THREAD_INVALID_STATE, "보관된 챗 스레드에는 메시지를 추가할 수 없습니다");
        }
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_THREAD_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_THREAD_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }

    private String requireContextType(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_CHAT_THREAD_CONTEXT_TYPE_REQUIRED, "문맥 타입은 필수입니다");
        }
        return value.trim();
    }

    private String requireTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_CHAT_THREAD_TITLE_REQUIRED, "스레드 제목은 필수입니다");
        }
        return value.trim();
    }

    private Instant requireLastMessageAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_CHAT_THREAD_LAST_MESSAGE_AT_REQUIRED, "마지막 메시지 시각은 필수입니다");
        }
        return value;
    }
}
