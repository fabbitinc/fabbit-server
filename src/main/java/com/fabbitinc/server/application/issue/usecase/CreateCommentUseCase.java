package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final ObjectMapper objectMapper;

    public CommentResult execute(CreateCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        java.util.UUID targetId = WorkItemUseCaseSupport.resolveIssueId(issueService, command.issueNumber());

        return WorkItemUseCaseSupport.toCommentResult(
                issueService.createComment(auth.userId(), targetId, command.body()),
                objectMapper
        );
    }

    public record CreateCommentCommand(
            int issueNumber,
            JsonNode body
    ) {
    }
}
