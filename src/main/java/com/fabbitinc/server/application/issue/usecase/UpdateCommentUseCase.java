package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.issue.service.IssueService;
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
public class UpdateCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final UserApi userApi;
    private final FileUrlResolver fileUrlResolver;
    private final ObjectMapper objectMapper;

    public CommentResult execute(UpdateCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        return WorkItemUseCaseSupport.toCommentResult(
                issueService.updateComment(auth.userId(), command.issueId(), command.commentId(), command.body()),
                objectMapper,
                userApi.getUserOrNull(auth.userId()),
                fileUrlResolver
        );
    }

    public record UpdateCommentCommand(
            UUID issueId,
            UUID commentId,
            JsonNode body
    ) {
    }
}
