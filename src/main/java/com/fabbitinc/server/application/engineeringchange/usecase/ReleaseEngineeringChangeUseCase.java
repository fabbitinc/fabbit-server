package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeAffectedItemApi;
import com.fabbitinc.server.application.engineeringchange.event.EngineeringChangeReleasedEvent;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class ReleaseEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final EngineeringChangeAffectedItemApi affectedItemApi;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void execute(ReleaseEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
        engineeringChangeService.approveStep(auth.userId(), engineeringChange, command.stepId());

        if (engineeringChange.getState() == EngineeringChangeState.RELEASE_PENDING) {
            affectedItemApi.releaseAffectedItems(
                    auth.userId(),
                    engineeringChange.getId()
            );
            engineeringChange.release(java.time.Instant.now(), auth.userId());
            applicationEventPublisher.publishEvent(new EngineeringChangeReleasedEvent(
                    engineeringChange.getId(),
                    auth.userId(),
                    engineeringChange.getNumber(),
                    engineeringChange.getTitle()
            ));
        }
    }

    public record ReleaseEngineeringChangeCommand(java.util.UUID engineeringChangeId, java.util.UUID stepId) {
    }
}
