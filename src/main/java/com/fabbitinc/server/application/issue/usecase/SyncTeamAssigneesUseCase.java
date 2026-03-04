package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.dto.request.SyncTeamAssigneesRequest;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SyncTeamAssigneesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    @Transactional
    public SyncDiffResponse execute(IssueTargetType targetType,
            int issueNumber,
            SyncTeamAssigneesRequest request
    ) {
        currentAuthProvider.getCurrentAuth();
        UUID issueId = resolveIssueId(targetType, issueNumber);

        IssueService.DiffResult diff = issueService.syncTeamAssignees(issueId, request.teamIds());
        return new SyncDiffResponse(diff.added().size(), diff.removed().size());
    }

    private UUID resolveIssueId(IssueTargetType targetType, int issueNumber) {
        if (targetType == IssueTargetType.CHANGE_REQUEST) {
            return issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();
        }
        return issueService.getIssueByNumberOrThrow(issueNumber).getId();
    }
}
