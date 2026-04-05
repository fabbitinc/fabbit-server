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
 * 변경안 작성자가 수정 요청된 단계를 재제출한다.
 * CHANGES_REQUESTED 상태의 step이 다시 PENDING으로 전환된다.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class ResubmitStepUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(ResubmitStepCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
        engineeringChangeService.resubmitStep(auth.userId(), engineeringChange, command.stepId());
    }

    public record ResubmitStepCommand(UUID engineeringChangeId, UUID stepId) {
    }
}
