package com.fabbitinc.server.application.chat.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.service.ChatActionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class RejectChatActionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatActionService chatActionService;

    public void execute(RejectChatActionCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        chatActionService.reject(command.actionRequestId(), auth.orgId(), auth.userId());
    }

    public record RejectChatActionCommand(
            UUID actionRequestId
    ) {
    }
}
