package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public void execute(DeleteCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = IssueUseCaseSupport.resolveIssueId(issueService, command.targetType(), command.issueNumber());

        issueService.deleteComment(auth.userId(), issueId, command.commentId());
    }

    public record DeleteCommentCommand(
            IssueTargetType targetType,
            int issueNumber,
            UUID commentId
    ) {
    }
}
