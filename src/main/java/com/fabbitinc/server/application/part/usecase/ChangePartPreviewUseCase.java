package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import com.fabbitinc.server.application.part.usecase.command.ChangePartPreviewCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ChangePartPreviewUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionRouteService partRevisionRouteService;
    private final PartPreviewService partPreviewService;

    public void execute(ChangePartPreviewCommand command) {
        currentAuthProvider.getCurrentAuth();
        partPreviewService.changeSource(
                partRevisionRouteService.getRequiredPartId(command.partNumber(), command.revisionCode()),
                command.sourceType(),
                command.sourceId()
        );
    }
}
