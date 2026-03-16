package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.IssueService;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncLinkedEngineeringChangesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;
    public SyncDiffResult execute(SyncLinkedEngineeringChangesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        UUID issueId = issueService.getIssueByNumberOrThrow(command.issueNumber()).getId();

        IssueService.DiffResult diff = issueService.syncLinkedEngineeringChanges(
                auth.userId(),
                issueId,
                command.engineeringChangeIds(),
                true
        );
        return WorkItemUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncLinkedEngineeringChangesCommand(
            int issueNumber,
            List<UUID> engineeringChangeIds
    ) {
        public SyncLinkedEngineeringChangesCommand {
            engineeringChangeIds = engineeringChangeIds == null ? List.of() : List.copyOf(engineeringChangeIds);
        }
    }
}
