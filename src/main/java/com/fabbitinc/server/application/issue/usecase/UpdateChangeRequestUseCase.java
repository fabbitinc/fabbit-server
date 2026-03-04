package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.dto.request.UpdateIssueRequest;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateChangeRequestUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public void execute(String authorizationHeader, int issueNumber, UpdateIssueRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(issueNumber);
        issueService.updateChangeRequest(auth.userId(), changeRequest, request.title(), request.body());
    }
}
