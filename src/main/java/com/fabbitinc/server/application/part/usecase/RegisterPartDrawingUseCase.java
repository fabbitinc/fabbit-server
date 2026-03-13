package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.result.RegisterPartDrawingResult;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RegisterPartDrawingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final DrawingService drawingService;

    public RegisterPartDrawingResult execute(RegisterPartDrawingCommand command) {
        currentAuthProvider.getCurrentAuth();

        Drawing drawing = drawingService.createDrawing(command.partId(), command.fileId());

        return new RegisterPartDrawingResult(
                drawing.getId(),
                drawing.getDrawingNumber(),
                drawing.getName(),
                null
        );
    }
}
