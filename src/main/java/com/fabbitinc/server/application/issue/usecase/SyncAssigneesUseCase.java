package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncAssigneesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncAssigneesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = IssueUseCaseSupport.resolveIssueId(issueService, command.targetType(), command.issueNumber());

        IssueService.DiffResult diff = issueService.syncAssignees(
                auth.userId(),
                issueId,
                command.userIds(),
                true
        );
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncAssigneesCommand(
            IssueTargetType targetType,
            int issueNumber,
            List<UUID> userIds
    ) {
        public SyncAssigneesCommand {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }
    }
}
