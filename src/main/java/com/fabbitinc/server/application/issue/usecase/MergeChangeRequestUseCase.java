package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.domain.issue.model.ChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MergeChangeRequestUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    @Transactional
    public void execute(int issueNumber) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChangeRequest changeRequest = issueService.getChangeRequestByNumberOrThrow(issueNumber);
        issueService.mergeChangeRequest(auth.userId(), changeRequest);
    }
}
