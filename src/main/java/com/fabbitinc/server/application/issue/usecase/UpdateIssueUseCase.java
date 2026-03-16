package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Component
@Transactional
@RequiredArgsConstructor
public class UpdateIssueUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public void execute(UpdateIssueCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Issue issue = issueService.getIssueByNumberOrThrow(command.issueNumber());
        issueService.updateIssue(auth.userId(), issue, command.title(), command.body());
    }

    public record UpdateIssueCommand(
            int issueNumber,
            String title,
            JsonNode body
    ) {
    }
}
