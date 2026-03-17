package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.application.part.usecase.command.DetachPartFileCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DetachPartFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    public void execute(DetachPartFileCommand command) {
        var auth = currentAuthProvider.getCurrentAuth();
        partService.detachFile(command.partId(), command.revisionId(), command.fileId(), auth.userId());
    }
}
