package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeletePartDrawingUseCase {

    private final AuthTokenParser authTokenParser;
    private final PartService partService;
    private final DrawingService drawingService;

    @Transactional
    public void execute(String authorizationHeader, UUID partId) {
        authTokenParser.requireAuth(authorizationHeader);
        UUID drawingId = partService.unassignDrawing(partId);
        drawingService.deleteDrawing(drawingId);
    }
}
