package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncIssuesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncIssuesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID changeRequestId = issueService.getChangeRequestByNumberOrThrow(command.issueNumber()).getId();

        IssueService.DiffResult diff = issueService.syncIssues(
                auth.userId(),
                changeRequestId,
                command.issueIds(),
                true
        );
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncIssuesCommand(
            int issueNumber,
            List<UUID> issueIds
    ) {
        public SyncIssuesCommand {
            issueIds = issueIds == null ? List.of() : List.copyOf(issueIds);
        }
    }
}
