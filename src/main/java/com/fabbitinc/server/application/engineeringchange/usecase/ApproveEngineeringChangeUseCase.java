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
public class ApproveEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(ApproveEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByNumberOrThrow(command.engineeringChangeNumber());
        engineeringChangeService.approveEngineeringChange(auth.userId(), engineeringChange);
    }

    public record ApproveEngineeringChangeCommand(int engineeringChangeNumber) {
    }
}
