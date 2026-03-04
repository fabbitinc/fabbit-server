package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.dto.request.SyncTeamReviewersRequest;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SyncTeamReviewersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    @Transactional
    public SyncDiffResponse execute(int issueNumber, SyncTeamReviewersRequest request) {
        currentAuthProvider.getCurrentAuth();
        UUID changeRequestId = issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();

        IssueService.DiffResult diff = issueService.syncTeamReviewers(changeRequestId, request.teamIds());
        return new SyncDiffResponse(diff.added().size(), diff.removed().size());
    }
}
