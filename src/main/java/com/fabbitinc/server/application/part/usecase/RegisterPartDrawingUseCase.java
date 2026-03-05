package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
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
@Transactional
public class RegisterPartDrawingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final DrawingService drawingService;
    private final PartService partService;

    public RegisterDrawingResponse execute(UUID partId, UUID fileId) {
        currentAuthProvider.getCurrentAuth();

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
