package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SubmitChangeRequestUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public void execute(SubmitChangeRequestCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(command.issueNumber());
        issueService.submitChangeRequest(auth.userId(), changeRequest);
        partRevisionWorkflowApi.submitChangeRequest(changeRequest.getId());
    }

    public record SubmitChangeRequestCommand(int issueNumber) {
    }
}
