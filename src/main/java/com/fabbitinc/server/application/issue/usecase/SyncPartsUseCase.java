package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
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
public class SyncPartsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final IssueService issueService;

    public SyncDiffResult execute(SyncPartsCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        IssueService.DiffResult diff = issueService.syncParts(auth.userId(), command.issueId(), command.partIds(), true);
        return WorkItemUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncPartsCommand(
            UUID issueId,
            List<UUID> partIds
    ) {
        public SyncPartsCommand {
            partIds = partIds == null ? List.of() : List.copyOf(partIds);
        }
    }
}
