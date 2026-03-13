package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import com.fabbitinc.server.application.part.usecase.command.ClearPartPreviewCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ClearPartPreviewUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionRouteService partRevisionRouteService;
    private final PartPreviewService partPreviewService;

    public void execute(ClearPartPreviewCommand command) {
        currentAuthProvider.getCurrentAuth();
        partPreviewService.clearByPart(
                partRevisionRouteService.getRequiredPartId(command.partNumber(), command.revisionCode())
        );
    }
}
