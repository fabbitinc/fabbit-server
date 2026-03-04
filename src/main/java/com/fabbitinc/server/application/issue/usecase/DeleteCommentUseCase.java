package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public void execute(
            String authorizationHeader,
            IssueTargetType targetType,
            int issueNumber,
            UUID commentId
    ) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        UUID issueId = resolveIssueId(targetType, issueNumber);

        issueService.deleteComment(auth.userId(), issueId, commentId);
    }

    private UUID resolveIssueId(IssueTargetType targetType, int issueNumber) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();
        }
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }
}
