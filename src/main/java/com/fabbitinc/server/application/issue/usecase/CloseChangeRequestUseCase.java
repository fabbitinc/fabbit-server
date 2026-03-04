package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CloseChangeRequestUseCase {

    private final AuthTokenParser authTokenParser;
    private final IssueService issueService;

    @Transactional
    public void execute(String authorizationHeader, int issueNumber) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(issueNumber);
        issueService.closeChangeRequest(auth.userId(), changeRequest);
    }
}
