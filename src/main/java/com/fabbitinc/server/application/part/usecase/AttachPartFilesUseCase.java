package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.usecase.command.AttachPartFilesCommand;
import com.fabbitinc.server.application.part.usecase.result.AttachPartFilesResult;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AttachPartFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionRouteService partRevisionRouteService;
    private final PartService partService;

    public AttachPartFilesResult execute(AttachPartFilesCommand command) {
        currentAuthProvider.getCurrentAuth();
        var partId = partRevisionRouteService.getRequiredPartId(command.partNumber(), command.revisionCode());
        return new AttachPartFilesResult(
                partService.attachFiles(partId, command.fileIds()).stream()
                        .map(File::getId)
                        .toList()
        );
    }
}
