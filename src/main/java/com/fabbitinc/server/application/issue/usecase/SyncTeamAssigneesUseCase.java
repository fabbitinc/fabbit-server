package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.issue.support.IssueTargetType;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncTeamAssigneesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncTeamAssigneesCommand command) {
        currentAuthProvider.getCurrentAuth();
        UUID issueId = IssueUseCaseSupport.resolveIssueId(issueService, command.targetType(), command.issueNumber());

        IssueService.DiffResult diff = issueService.syncTeamAssignees(issueId, command.teamIds());
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncTeamAssigneesCommand(
            IssueTargetType targetType,
            int issueNumber,
            List<UUID> teamIds
    ) {
        public SyncTeamAssigneesCommand {
            teamIds = teamIds == null ? List.of() : List.copyOf(teamIds);
        }
    }
}
