package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public void execute(DeleteCommentCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID targetId = IssueUseCaseSupport.resolveIssueId(issueService, command.issueNumber());

        issueService.deleteComment(auth.userId(), targetId, command.commentId());
    }

    public record DeleteCommentCommand(
            int issueNumber,
            UUID commentId
    ) {
    }
}
