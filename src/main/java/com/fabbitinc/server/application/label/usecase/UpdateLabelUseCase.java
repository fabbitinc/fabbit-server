package com.fabbitinc.server.application.label.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.label.service.LabelService;
import com.fabbitinc.server.application.label.usecase.command.UpdateLabelCommand;
import com.fabbitinc.server.application.label.usecase.result.UpdateLabelResult;
import com.fabbitinc.server.domain.label.model.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateLabelUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final LabelService labelService;

    public UpdateLabelResult execute(UpdateLabelCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Label label = labelService.updateLabel(
                auth.userId(),
                command.labelId(),
                command.name(),
                command.description(),
                command.color(),
                command.descriptionSet()
        );
        return new UpdateLabelResult(
                label.getId(),
                label.getName(),
                label.getDescription(),
                label.getColor(),
                label.getCreatedAt(),
                label.getCreatedBy()
        );
    }
}
