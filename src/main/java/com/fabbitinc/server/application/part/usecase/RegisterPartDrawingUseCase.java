package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RegisterPartDrawingUseCase {

    private final AuthTokenParser authTokenParser;
    private final DrawingService drawingService;
    private final PartService partService;

    @Transactional
    public RegisterDrawingResponse execute(String authorizationHeader, UUID partId, UUID fileId) {
        authTokenParser.requireAuth(authorizationHeader);

        Drawing drawing = drawingService.createDrawing(fileId);
        partService.assignDrawing(partId, drawing.getId());

        return new RegisterDrawingResponse(
                drawing.getId(),
                drawing.getDrawingNumber(),
                drawing.getName(),
                drawing.getConversionStatus()
        );
    }
}
