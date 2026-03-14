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
public class MergeChangeRequestUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public void execute(MergeChangeRequestCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(command.issueNumber());
        partRevisionWorkflowApi.mergeChangeRequest(
                auth.userId(),
                changeRequest.getId(),
                changeRequest.getNumber(),
                changeRequest.getTitle()
        );
        issueService.mergeChangeRequest(auth.userId(), changeRequest);
    }

    public record MergeChangeRequestCommand(int issueNumber) {
    }
}
