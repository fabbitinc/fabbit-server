package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
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
        IssueService.DiffResult diff = issueService.syncTeamAssignees(command.issueId(), command.teamIds());
        return WorkItemUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncTeamAssigneesCommand(
            UUID issueId,
            List<UUID> teamIds
    ) {
        public SyncTeamAssigneesCommand {
            teamIds = teamIds == null ? List.of() : List.copyOf(teamIds);
        }
    }
}
