package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import java.time.Instant;
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
        engineeringChangeService.replaceStages(
                auth.userId(),
                engineeringChange,
                command.stages().stream()
                        .map(stage -> new EngineeringChangeService.StageDraft(
                                stage.stepType(),
                                stage.sequence(),
                                stage.completionPolicy(),
                                stage.minApprovals(),
                                stage.deadline(),
                                stage.assignees().stream()
                                        .map(a -> new EngineeringChangeService.StepAssigneeDraft(
                                                a.assigneeType(),
                                                a.assigneeId()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    public record ReplaceEngineeringChangeStepsCommand(
            UUID engineeringChangeId,
            List<StageItem> stages
    ) {
        public ReplaceEngineeringChangeStepsCommand {
            stages = stages == null ? List.of() : List.copyOf(stages);
        }

        public record StageItem(
                EngineeringChangeStepType stepType,
                int sequence,
                StepStageCompletionPolicy completionPolicy,
                Integer minApprovals,
                Instant deadline,
                List<AssigneeItem> assignees
        ) {
            public StageItem {
                assignees = assignees == null ? List.of() : List.copyOf(assignees);
            }
        }

        public record AssigneeItem(
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId
        ) {
        }
    }
}
