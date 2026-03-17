package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import com.fabbitinc.server.application.part.usecase.command.DeletePartPreviewFileCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePartPreviewFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartPreviewService partPreviewService;

    public void execute(DeletePartPreviewFileCommand command) {
        var auth = currentAuthProvider.getCurrentAuth();
        partPreviewService.deletePreviewFile(command.revisionId(), command.previewFileId(), auth.userId());
    }
}
