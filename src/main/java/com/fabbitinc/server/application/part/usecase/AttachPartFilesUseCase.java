package com.fabbitinc.server.application.part.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.service.PartService;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AttachPartFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartService partService;

    @Transactional
    public List<UUID> execute(UUID partId, List<UUID> fileIds) {
        currentAuthProvider.getCurrentAuth();
        return partService.attachFiles(partId, fileIds).stream()
                .map(File::getId)
                .toList();
    }
}
