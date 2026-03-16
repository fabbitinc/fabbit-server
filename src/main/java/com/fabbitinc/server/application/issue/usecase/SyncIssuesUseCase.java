package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.EngineeringChangeService;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncIssuesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public SyncDiffResult execute(SyncIssuesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID engineeringChangeId =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.issueNumber()).getId();

        EngineeringChangeService.DiffResult diff = engineeringChangeService.syncIssues(
                auth.userId(),
                engineeringChangeId,
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
