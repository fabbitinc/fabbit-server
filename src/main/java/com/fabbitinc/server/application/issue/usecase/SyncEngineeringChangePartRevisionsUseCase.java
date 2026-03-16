package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.EngineeringChangeService;
import com.fabbitinc.server.application.issue.usecase.result.SyncDiffResult;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionRef;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.issue.model.EngineeringChange;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncEngineeringChangePartRevisionsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public SyncDiffResult execute(SyncEngineeringChangePartRevisionsCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.issueNumber());
        PartRevisionWorkflowApi.DiffResult diff = partRevisionWorkflowApi.syncEngineeringChangePartRevisions(
                engineeringChange.getId(),
                command.items().stream()
                        .map(item -> new EngineeringChangePartRevisionRef(
                                item.partNumber(),
                                item.baseRevisionCode(),
                                item.draftKey()
                        ))
                        .toList()
        );
        engineeringChangeService.recordEngineeringChangePartRevisionDiffActivity(
                auth.userId(),
                engineeringChange.getId(),
                diff.addedItems(),
                diff.removedItems()
        );
        return new SyncDiffResult(diff.addedCount(), diff.removedCount());
    }

    public record SyncEngineeringChangePartRevisionsCommand(
            int issueNumber,
            List<Item> items
    ) {
        public SyncEngineeringChangePartRevisionsCommand {
            items = items == null ? List.of() : List.copyOf(items);
        }

        public record Item(
                String partNumber,
                String baseRevisionCode,
                String draftKey
        ) {
        }
    }
}
