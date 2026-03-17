package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class UpdateEngineeringChangeCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final UserApi userApi;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;

    public CommentResult execute(UpdateEngineeringChangeCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        return WorkItemUseCaseSupport.toCommentResult(
                engineeringChangeService.updateComment(
                        auth.userId(),
                        command.engineeringChangeId(),
                        command.commentId(),
                        command.body()
                ),
                objectMapper,
                userApi.getUserOrNull(auth.userId()),
                fileUrlResolver
        );
    }

    public record UpdateEngineeringChangeCommentCommand(
            UUID engineeringChangeId,
            UUID commentId,
            JsonNode body
    ) {
    }
}
