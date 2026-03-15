package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.drawing.service.DrawingService;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
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
    private final PartRevisionRouteService partRevisionRouteService;
    private final DrawingService drawingService;

    public RegisterPartDrawingResult execute(RegisterPartDrawingCommand command) {
        currentAuthProvider.getCurrentAuth();
        PartRevision revision = resolveTargetRevision(command);

        Drawing drawing = drawingService.createDrawing(
                revision.getId(),
                command.fileId()
        );

        return new RegisterPartDrawingResult(
                drawing.getId(),
                drawing.getDrawingNumber(),
                drawing.getName()
        );
    }

    private PartRevision resolveTargetRevision(RegisterPartDrawingCommand command) {
        if (command.draftKey() == null || command.draftKey().isBlank()) {
            return partRevisionRouteService.getRequiredRevision(command.partNumber(), command.revisionCode());
        }

        PartRevision draft = partRevisionRouteService.getRequiredDraft(
                command.partNumber(),
                command.revisionCode(),
                command.draftKey()
        );
        draft.assertDraftEditable();
        return draft;
    }
}
