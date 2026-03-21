package com.fabbitinc.server.application.chat.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.service.ChatActionService;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ConfirmChatActionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatActionService chatActionService;

    public ConfirmChatActionResult execute(ConfirmChatActionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChatActionService.ConfirmedActionResult result = chatActionService.confirm(
                command.actionRequestId(),
                auth.orgId(),
                auth.userId()
        );
        return new ConfirmChatActionResult(result.actionRequestId(), result.status(), result.issueId());
    }

    public record ConfirmChatActionCommand(
            UUID actionRequestId
    ) {
    }

    public record ConfirmChatActionResult(
            UUID actionRequestId,
            ChatActionRequestStatus status,
            UUID issueId
    ) {
    }
}
