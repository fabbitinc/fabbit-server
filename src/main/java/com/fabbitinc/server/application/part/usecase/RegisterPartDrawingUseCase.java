package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.result.RegisterPartDrawingResult;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.part.model.PartRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RegisterPartDrawingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionService partRevisionService;
    private final DrawingService drawingService;

    public RegisterPartDrawingResult execute(RegisterPartDrawingCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartRevision revision = partRevisionService.getRequiredEditableRevision(command.partId(), command.revisionId());

        Drawing drawing = drawingService.createDrawing(revision.getId(), command.fileId());
        return new RegisterPartDrawingResult(drawing.getId(), drawing.getDrawingNumber(), drawing.getName());
    }
}
