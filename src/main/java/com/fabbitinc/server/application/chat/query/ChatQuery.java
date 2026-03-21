package com.fabbitinc.server.application.chat.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.query.condition.ChatMessageListCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatThreadDetailCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatThreadListCondition;
import com.fabbitinc.server.application.chat.query.result.ChatMessageListResult;
import com.fabbitinc.server.application.chat.query.result.ChatThreadDetailResult;
import com.fabbitinc.server.application.chat.query.result.ChatThreadListResult;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.chat.model.ChatThread;
import com.fabbitinc.server.domain.chat.repository.ChatMessageRepository;
import com.fabbitinc.server.domain.chat.repository.ChatThreadRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatThreadListResult list(ChatThreadListCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        List<ChatThreadListResult.Item> items = chatThreadRepository.findByOrgIdAndUserIdOrderByLastMessageAtDesc(
                        auth.orgId(),
                        auth.userId()
                ).stream()
                .map(thread -> new ChatThreadListResult.Item(
                        thread.getId(),
                        thread.getProjectId(),
                        thread.getContextType(),
                        thread.getContextId(),
                        thread.getTitle(),
                        thread.getStatus(),
                        thread.getLastMessageAt(),
                        thread.getCreatedAt()
                ))
                .toList();
        return new ChatThreadListResult(items);
    }

    public ChatThreadDetailResult get(ChatThreadDetailCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChatThread thread = chatThreadRepository.findByIdAndOrgIdAndUserId(condition.threadId(), auth.orgId(), auth.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));
        return new ChatThreadDetailResult(
                thread.getId(),
                thread.getProjectId(),
                thread.getContextType(),
                thread.getContextId(),
                thread.getTitle(),
                thread.getStatus(),
                thread.getLastMessageAt(),
                thread.getCreatedAt()
        );
    }

    public ChatMessageListResult list(ChatMessageListCondition condition) {
        get(new ChatThreadDetailCondition(condition.threadId()));
        List<ChatMessageListResult.Item> items = chatMessageRepository.findByThreadIdOrderBySequenceAsc(condition.threadId()).stream()
                .map(message -> new ChatMessageListResult.Item(
                        message.getId(),
                        message.getRunId(),
                        message.getRole(),
                        message.getMessageType(),
                        message.getStatus(),
                        message.getSequence(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();
        return new ChatMessageListResult(items);
    }
}
