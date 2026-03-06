package com.fabbitinc.server.application.label.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.label.service.LabelService;
import com.fabbitinc.server.application.label.usecase.command.DeleteLabelCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeleteLabelUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final LabelService labelService;

    public void execute(DeleteLabelCommand command) {
        currentAuthProvider.getCurrentAuth();
        labelService.deleteLabel(command.labelId());
    }
}
