package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Component
@Transactional
@RequiredArgsConstructor
public class UpdateEngineeringChangeUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(UpdateEngineeringChangeCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
        engineeringChangeService.updateEngineeringChange(auth.userId(), engineeringChange, command.title(), command.body());
        if (command.steps() != null) {
            engineeringChangeService.replaceSteps(
                    auth.userId(),
                    engineeringChange,
                    command.steps().stream()
                            .map(step -> new EngineeringChangeService.StepDraft(
                                    step.stepType(),
                                    step.assigneeType(),
                                    step.assigneeId(),
                                    step.sequence()
                            ))
                            .toList(),
                    true
            );
        }
    }

    public record UpdateEngineeringChangeCommand(
            UUID engineeringChangeId,
            String title,
            JsonNode body,
            List<StepTarget> steps
    ) {
        public UpdateEngineeringChangeCommand {
            steps = steps == null ? null : List.copyOf(steps);
        }

        public record StepTarget(
                EngineeringChangeStepType stepType,
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId,
                int sequence
        ) {
        }
    }
}
