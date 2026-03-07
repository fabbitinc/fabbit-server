package com.fabbitinc.server.application.issue.usecase;

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
public class SyncTeamReviewersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncTeamReviewersCommand command) {
        currentAuthProvider.getCurrentAuth();
        UUID changeRequestId = issueService.getChangeRequestByNumberOrThrow(command.issueNumber()).getId();

        IssueService.DiffResult diff = issueService.syncTeamReviewers(changeRequestId, command.teamIds());
        return IssueUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncTeamReviewersCommand(
            int issueNumber,
            List<UUID> teamIds
    ) {
        public SyncTeamReviewersCommand {
            teamIds = teamIds == null ? List.of() : List.copyOf(teamIds);
        }
    }
}
