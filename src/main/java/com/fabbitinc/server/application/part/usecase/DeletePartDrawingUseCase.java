package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePartDrawingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final DrawingService drawingService;

    public void execute(DeletePartDrawingCommand command) {
        var auth = currentAuthProvider.getCurrentAuth();
        drawingService.deleteDrawing(command.revisionId(), command.drawingId(), auth.userId());
    }
}
