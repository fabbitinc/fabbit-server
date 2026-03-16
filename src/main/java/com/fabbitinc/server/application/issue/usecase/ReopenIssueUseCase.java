package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ReopenIssueUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public void execute(ReopenIssueCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Issue issue = issueService.getIssueByNumberOrThrow(command.issueNumber());
        issueService.reopenIssue(auth.userId(), issue);
    }

    public record ReopenIssueCommand(int issueNumber) {
    }
}
