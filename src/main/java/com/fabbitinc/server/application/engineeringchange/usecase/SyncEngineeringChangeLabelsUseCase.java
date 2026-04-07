package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
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
public class SyncEngineeringChangeLabelsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public SyncDiffResult execute(SyncEngineeringChangeLabelsCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChangeService.DiffResult diff = engineeringChangeService.syncLabels(
                auth.userId(),
                command.engineeringChangeId(),
                command.labelIds(),
                true
        );
        return WorkItemUseCaseSupport.toSyncDiffResult(diff);
    }

    public record SyncEngineeringChangeLabelsCommand(
            UUID engineeringChangeId,
            List<UUID> labelIds
    ) {
        public SyncEngineeringChangeLabelsCommand {
            labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
        }
    }
}
