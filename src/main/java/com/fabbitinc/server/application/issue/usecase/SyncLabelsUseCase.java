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
public class SyncLabelsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncLabelsCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = IssueUseCaseSupport.resolveIssueId(issueService, command.issueNumber());

        IssueService.DiffResult diff = issueService.syncLabels(auth.userId(), issueId, command.labelIds(), true);
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncLabelsCommand(
            int issueNumber,
            List<UUID> labelIds
    ) {
        public SyncLabelsCommand {
            labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
        }
    }
}
