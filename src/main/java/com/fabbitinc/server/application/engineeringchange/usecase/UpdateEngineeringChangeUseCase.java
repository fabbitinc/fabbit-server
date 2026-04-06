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
        if (command.stages() != null) {
            engineeringChangeService.syncStages(
                    auth.userId(),
                    engineeringChange,
                    command.stages().stream()
                            .map(stage -> new EngineeringChangeService.StageDraft(
                                    null,
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
    }

    public record UpdateEngineeringChangeCommand(
            UUID engineeringChangeId,
            String title,
            JsonNode body,
            List<StageTarget> stages
    ) {
        public UpdateEngineeringChangeCommand {
            stages = stages == null ? null : List.copyOf(stages);
        }

        public record StageTarget(
                EngineeringChangeStepType stepType,
                int sequence,
                StepStageCompletionPolicy completionPolicy,
                Integer minApprovals,
                Instant deadline,
                List<AssigneeTarget> assignees
        ) {
            public StageTarget {
                assignees = assignees == null ? List.of() : List.copyOf(assignees);
            }
        }

        public record AssigneeTarget(
                EngineeringChangeStepAssigneeType assigneeType,
                UUID assigneeId
        ) {
        }
    }
}
