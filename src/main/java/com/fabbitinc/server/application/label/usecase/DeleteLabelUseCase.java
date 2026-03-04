package com.fabbitinc.server.application.label.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.label.service.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteLabelUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final LabelService labelService;

    @Transactional
    public void execute(UUID labelId) {
        currentAuthProvider.getCurrentAuth();
        labelService.deleteLabel(labelId);
    }
}
