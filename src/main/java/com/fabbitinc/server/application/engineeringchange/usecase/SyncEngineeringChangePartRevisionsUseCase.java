package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.workitem.usecase.WorkItemUseCaseSupport;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import com.fabbitinc.server.application.part.api.EngineeringChangePartRevisionRef;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
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
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.engineeringChangeNumber());
        if (engineeringChange.getState() != EngineeringChangeState.DRAFT) {
            throw new AppException(
                    ErrorCode.INVALID_STATE,
                    "DRAFT 상태의 EngineeringChange에서만 대상 리비전을 변경할 수 있습니다"
            );
        }
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
            int engineeringChangeNumber,
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
