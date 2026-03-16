package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.usecase.result.CommentResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class UpdateCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final ObjectMapper objectMapper;

    public CommentResult execute(UpdateCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID targetId = IssueUseCaseSupport.resolveIssueId(issueService, command.issueNumber());

        return IssueUseCaseSupport.toCommentResult(
                issueService.updateComment(auth.userId(), targetId, command.commentId(), command.body()),
                objectMapper
        );
    }

    public record UpdateCommentCommand(
            int issueNumber,
            UUID commentId,
            JsonNode body
    ) {
    }
}
