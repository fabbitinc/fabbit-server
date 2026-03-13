package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePartDrawingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionRouteService partRevisionRouteService;
    private final DrawingService drawingService;

    public void execute(DeletePartDrawingCommand command) {
        currentAuthProvider.getCurrentAuth();
        drawingService.deleteDrawing(
                partRevisionRouteService.getRequiredPartId(command.partNumber(), command.revisionCode()),
                command.drawingId()
        );
    }
}
