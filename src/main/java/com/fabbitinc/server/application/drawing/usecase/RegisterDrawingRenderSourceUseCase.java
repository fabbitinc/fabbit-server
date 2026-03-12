package com.fabbitinc.server.application.drawing.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.drawing.usecase.command.RegisterDrawingRenderSourceCommand;
import com.fabbitinc.server.application.drawing.usecase.result.RegisterDrawingRenderSourceResult;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RegisterDrawingRenderSourceUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final DrawingService drawingService;

    public RegisterDrawingRenderSourceResult execute(RegisterDrawingRenderSourceCommand command) {
        currentAuthProvider.getCurrentAuth();

        Drawing drawing = drawingService.registerRenderSource(command.drawingId(), command.fileId());
        return new RegisterDrawingRenderSourceResult(
                drawing.getId(),
                drawing.getConversionStatus()
        );
    }
}
