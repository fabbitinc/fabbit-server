package com.fabbitinc.server.application.label.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.label.dto.request.CreateLabelRequest;
import com.fabbitinc.server.application.label.dto.response.LabelResponse;
import com.fabbitinc.server.application.label.service.LabelService;
import com.fabbitinc.server.domain.label.model.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateLabelUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final LabelService labelService;

    @Transactional
    public LabelResponse execute(CreateLabelRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Label label = labelService.createLabel(
                auth.userId(),
                request.name(),
                request.color(),
                request.description()
        );
        return new LabelResponse(
                label.getId(),
                label.getName(),
                label.getDescription(),
                label.getColor(),
                label.getCreatedAt(),
                label.getCreatedBy()
        );
    }
}
