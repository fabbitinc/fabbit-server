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

@Component
@Transactional
@RequiredArgsConstructor
public class ReplaceEngineeringChangeStepsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;

    public void execute(ReplaceEngineeringChangeStepsCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
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

    public record ReplaceEngineeringChangeStepsCommand(
            UUID engineeringChangeId,
            List<Item> steps
    ) {
        public ReplaceEngineeringChangeStepsCommand {
            steps = steps == null ? List.of() : List.copyOf(steps);
        }

        public record Item(
                EngineeringChangeStepType stepType,
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId,
                int sequence
        ) {
        }
    }
}
