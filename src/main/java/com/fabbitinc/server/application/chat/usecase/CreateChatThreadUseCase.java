package com.fabbitinc.server.application.chat.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.domain.chat.model.ChatThread;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateChatThreadUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatService chatService;

    public CreateChatThreadResult execute(CreateChatThreadCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        String resolvedContextType = command.contextType() == null || command.contextType().isBlank()
                ? "GLOBAL"
                : command.contextType().trim();
        String resolvedTitle = command.title() == null || command.title().isBlank()
                ? "새 챗"
                : command.title().trim();
        ChatThread thread = chatService.createThread(
                auth.orgId(),
                auth.userId(),
                command.projectId(),
                resolvedContextType,
                command.contextId(),
                resolvedTitle
        );
        return new CreateChatThreadResult(thread.getId());
    }

    public record CreateChatThreadCommand(
            UUID projectId,
            String contextType,
            UUID contextId,
            String title
    ) {
    }

    public record CreateChatThreadResult(
            UUID threadId
    ) {
    }
}
