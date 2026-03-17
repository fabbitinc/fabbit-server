package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class DeleteEngineeringChangeCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(DeleteEngineeringChangeCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        engineeringChangeService.deleteComment(
                auth.userId(),
                command.engineeringChangeId(),
                command.commentId()
        );
    }

    public record DeleteEngineeringChangeCommentCommand(
            UUID engineeringChangeId,
            UUID commentId
    ) {
    }
}
