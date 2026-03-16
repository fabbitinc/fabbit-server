package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.EngineeringChangeService;
import com.fabbitinc.server.application.issue.usecase.result.CommentResult;
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
    private final ObjectMapper objectMapper;

    public CommentResult execute(CreateEngineeringChangeCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        return IssueUseCaseSupport.toCommentResult(
                engineeringChangeService.createComment(
                        auth.userId(),
                        IssueUseCaseSupport.resolveEngineeringChangeId(engineeringChangeService, command.issueNumber()),
                        command.body()
                ),
                objectMapper
        );
    }

    public record CreateEngineeringChangeCommentCommand(
            int issueNumber,
            JsonNode body
    ) {
    }
}
