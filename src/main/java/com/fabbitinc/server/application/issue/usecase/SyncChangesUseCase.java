package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncChangesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncChangesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = issueService.getIssueByNumberOrThrow(command.issueNumber()).getId();

        IssueService.DiffResult diff = issueService.syncChanges(
                auth.userId(),
                issueId,
                command.changeRequestIds(),
                true
        );
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncChangesCommand(
            int issueNumber,
            List<UUID> changeRequestIds
    ) {
        public SyncChangesCommand {
            changeRequestIds = changeRequestIds == null ? List.of() : List.copyOf(changeRequestIds);
        }
    }
}
