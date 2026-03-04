package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.dto.request.UpdateIssueRequest;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateIssueUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    @Transactional
    public void execute(int issueNumber, UpdateIssueRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Issue issue = issueService.getIssueByNumberOrThrow(issueNumber);
        issueService.updateIssue(auth.userId(), issue, request.title(), request.body());
    }
}
