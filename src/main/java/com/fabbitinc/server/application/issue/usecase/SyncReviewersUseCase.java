package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.dto.request.SyncReviewersRequest;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SyncReviewersUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public SyncDiffResponse execute(String authorizationHeader, int issueNumber, SyncReviewersRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        UUID changeRequestId = issueService.getChangeRequestByNumberOrThrow(issueNumber).getId();

        IssueService.DiffResult diff = issueService.syncReviewers(
                auth.userId(),
                changeRequestId,
                request.userIds(),
                true
        );
        return new SyncDiffResponse(diff.added().size(), diff.removed().size());
    }
}
