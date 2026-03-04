package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.dto.request.UpdateIssueRequest;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateIssueUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public void execute(String authorizationHeader, int issueNumber, UpdateIssueRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        Issue issue = issueService.getIssueByNumberOrThrow(issueNumber);
        issueService.updateIssue(auth.userId(), issue, request.title(), request.body());
    }
}
