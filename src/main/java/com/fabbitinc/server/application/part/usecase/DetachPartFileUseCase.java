package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class DetachPartFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    public void execute(UUID partId, UUID fileId) {
        currentAuthProvider.getCurrentAuth();
        partService.detachFile(partId, fileId);
    }
}
