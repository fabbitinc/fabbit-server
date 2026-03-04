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
@RequiredArgsConstructor
public class DeleteIssueFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    @Transactional
    public void execute(IssueTargetType targetType,
            int issueNumber,
            UUID fileId
    ) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = resolveIssueId(targetType, issueNumber);

        issueService.detachFile(auth.userId(), issueId, fileId);
    }

    private UUID resolveIssueId(IssueTargetType targetType, int issueNumber) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();
        }
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }
}
