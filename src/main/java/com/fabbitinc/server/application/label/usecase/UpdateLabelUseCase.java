package com.fabbitinc.server.application.label.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.label.dto.request.UpdateLabelRequest;
import com.fabbitinc.server.application.label.dto.response.LabelResponse;
import com.fabbitinc.server.application.label.service.LabelService;
import com.fabbitinc.server.domain.label.model.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateLabelUseCase {

    private final AuthTokenParser authTokenParser;
    private final LabelService labelService;

    @Transactional
    public LabelResponse execute(
            String authorizationHeader,
            UUID labelId,
            UpdateLabelRequest request
    ) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        Label label = labelService.updateLabel(
                auth.userId(),
                labelId,
                request.getName(),
                request.getDescription(),
                request.getColor(),
                request.isDescriptionSet()
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
