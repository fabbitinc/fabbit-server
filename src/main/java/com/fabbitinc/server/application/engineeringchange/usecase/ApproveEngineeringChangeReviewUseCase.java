package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ApproveEngineeringChangeReviewUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(ApproveEngineeringChangeReviewCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
        engineeringChangeService.approveStep(auth.userId(), engineeringChange, command.stepId());
    }

    public record ApproveEngineeringChangeReviewCommand(java.util.UUID engineeringChangeId, java.util.UUID stepId) {
    }
}
