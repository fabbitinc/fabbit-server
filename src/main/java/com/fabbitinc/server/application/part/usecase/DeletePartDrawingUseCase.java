package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePartDrawingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;
    private final DrawingService drawingService;

    public void execute(DeletePartDrawingCommand command) {
        currentAuthProvider.getCurrentAuth();
        java.util.UUID drawingId = partService.unassignDrawing(command.partId());
        drawingService.deleteDrawing(drawingId);
    }
}
