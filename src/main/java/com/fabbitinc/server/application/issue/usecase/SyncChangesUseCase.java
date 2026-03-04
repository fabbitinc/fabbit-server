package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.dto.request.SyncChangesRequest;
import com.fabbitinc.server.application.issue.dto.response.SyncDiffResponse;
import com.fabbitinc.server.application.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SyncChangesUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public SyncDiffResponse execute(String authorizationHeader, int issueNumber, SyncChangesRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        UUID issueId = issueService.getIssueByNumberOrThrow(issueNumber).getId();

        IssueService.DiffResult diff = issueService.syncChanges(
                auth.userId(),
                issueId,
                request.crIds(),
                true
        );
        return new SyncDiffResponse(diff.added().size(), diff.removed().size());
    }
}
