package com.fabbitinc.server.application.chat.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.service.ChatAgentService;
import com.fabbitinc.server.application.chat.service.ChatAsyncExecutionService;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.chat.model.ChatIntent;
import com.fabbitinc.server.domain.chat.model.ChatThread;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Transactional
@RequiredArgsConstructor
public class SendChatMessageUseCase {

    private static final String DEFAULT_MODEL = "chat-scaffold-v1";

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatService chatService;
    private final ChatMessageComposer chatMessageComposer;
    private final ChatAgentService chatAgentService;
    private final ChatAsyncExecutionService chatAsyncExecutionService;

    public SendChatMessageResult execute(SendChatMessageCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChatThread thread = chatService.getThreadOrThrow(command.threadId(), auth.orgId(), auth.userId());
        String userContent = chatMessageComposer.userText(command.text());
        ChatIntent intent = chatAgentService.detectIntent(command.text());
        ChatService.PreparedRun prepared = chatService.prepareRun(thread, userContent, DEFAULT_MODEL, intent, "{}");
        dispatchAfterCommit(prepared.run().getId());
        return new SendChatMessageResult(prepared.userMessage().getId(), prepared.run().getId(), prepared.run().getStatus().name());
    }

    private void dispatchAfterCommit(UUID runId) {
        String schemaName = TenantContextHolder.getCurrentSchema();
        Runnable dispatch = () -> chatAsyncExecutionService.runAsync(runId, schemaName);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }
        dispatch.run();
    }

    public record SendChatMessageCommand(
            UUID threadId,
            String text
    ) {
    }

    public record SendChatMessageResult(
            UUID messageId,
            UUID runId,
            String status
    ) {
    }
}
