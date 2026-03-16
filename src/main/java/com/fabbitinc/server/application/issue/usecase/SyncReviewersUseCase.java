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
public class SyncReviewersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public SyncDiffResult execute(SyncReviewersCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID engineeringChangeId =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.issueNumber()).getId();

        EngineeringChangeService.DiffResult diff = engineeringChangeService.syncReviewers(
                auth.userId(),
                engineeringChangeId,
                command.userIds(),
                true
        );
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncReviewersCommand(
            int issueNumber,
            List<UUID> userIds
    ) {
        public SyncReviewersCommand {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }
    }
}
