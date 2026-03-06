package com.fabbitinc.server.application.label.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.label.service.LabelService;
import com.fabbitinc.server.application.label.usecase.command.CreateLabelCommand;
import com.fabbitinc.server.application.label.usecase.result.CreateLabelResult;
import com.fabbitinc.server.domain.label.model.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreateLabelUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final LabelService labelService;

    public CreateLabelResult execute(CreateLabelCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Label label = labelService.createLabel(
                auth.userId(),
                command.name(),
                command.color(),
                command.description()
        );
        return new CreateLabelResult(
                label.getId(),
                label.getName(),
                label.getDescription(),
                label.getColor(),
                label.getCreatedAt(),
                label.getCreatedBy()
        );
    }
}
