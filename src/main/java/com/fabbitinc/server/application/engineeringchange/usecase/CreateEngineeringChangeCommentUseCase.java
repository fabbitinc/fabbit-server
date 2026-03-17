package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateEngineeringChangeCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final UserApi userApi;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;

    public CommentResult execute(CreateEngineeringChangeCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        return WorkItemUseCaseSupport.toCommentResult(
                engineeringChangeService.createComment(
                        auth.userId(),
                        command.engineeringChangeId(),
                        command.body()
                ),
                objectMapper,
                userApi.getUserOrNull(auth.userId()),
                fileUrlResolver
        );
    }

    public record CreateEngineeringChangeCommentCommand(
            java.util.UUID engineeringChangeId,
            JsonNode body
    ) {
    }
}
