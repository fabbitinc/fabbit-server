package com.fabbitinc.server.application.issue.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.issue.service.EngineeringChangeService;
import com.fabbitinc.server.application.part.api.PartRevisionWorkflowApi;
import com.fabbitinc.server.domain.issue.model.EngineeringChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class MergeEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final PartRevisionWorkflowApi partRevisionWorkflowApi;

    public void execute(MergeEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.issueNumber());
        partRevisionWorkflowApi.mergeEngineeringChange(
                auth.userId(),
                engineeringChange.getId(),
                engineeringChange.getNumber(),
                engineeringChange.getTitle()
        );
        engineeringChangeService.mergeEngineeringChange(auth.userId(), engineeringChange);
    }

    public record MergeEngineeringChangeCommand(int issueNumber) {
    }
}
