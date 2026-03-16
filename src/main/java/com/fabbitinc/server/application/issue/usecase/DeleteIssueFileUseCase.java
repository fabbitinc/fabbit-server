package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class DeleteIssueFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public void execute(DeleteIssueFileCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = IssueUseCaseSupport.resolveIssueId(issueService, command.issueNumber());

        issueService.detachFile(auth.userId(), issueId, command.fileId());
    }

    public record DeleteIssueFileCommand(
            int issueNumber,
            UUID fileId
    ) {
    }
}
