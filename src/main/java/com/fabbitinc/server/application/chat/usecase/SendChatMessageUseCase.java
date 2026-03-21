package com.fabbitinc.server.application.chat.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.service.ChatAgentService;
import com.fabbitinc.server.application.chat.service.ChatAsyncExecutionService;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
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

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;
    private final ChatService chatService;
    private final ChatMessageComposer chatMessageComposer;
    private final ChatAgentService chatAgentService;
    private final ChatAsyncExecutionService chatAsyncExecutionService;

    public SendChatMessageResult execute(SendChatMessageCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        chatAgentService.ensureAvailable();
        organizationApi.checkCreditQuota(auth.orgId(), AiUsageCategory.CHAT);
        ChatThread thread = chatService.getThreadForUpdateOrThrow(command.threadId(), auth.orgId(), auth.userId());
        String userContent = chatMessageComposer.userText(command.text());
        ChatService.PreparedRun prepared = chatService.prepareRun(
                thread,
                userContent,
                chatAgentService.getModelName(),
                ChatIntent.GENERAL_CHAT,
                "{}"
        );
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
