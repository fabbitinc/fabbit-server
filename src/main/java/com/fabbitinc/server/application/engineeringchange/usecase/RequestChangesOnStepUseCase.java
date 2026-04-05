package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단계 담당자가 수정 요청을 보낸다.
 * 해당 step이 CHANGES_REQUESTED 상태로 전환되며, 작성자에게 재제출을 요구한다.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class RequestChangesOnStepUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(RequestChangesOnStepCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
        engineeringChangeService.requestChangesOnStep(
                auth.userId(), engineeringChange, command.stepId(), command.comment()
        );
    }

    public record RequestChangesOnStepCommand(UUID engineeringChangeId, UUID stepId, String comment) {
    }
}
