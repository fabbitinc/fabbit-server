package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ClearPartPreviewUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartPreviewService partPreviewService;

    public void execute(UUID partId) {
        currentAuthProvider.getCurrentAuth();
        partPreviewService.clearByPart(partId);
    }
}
