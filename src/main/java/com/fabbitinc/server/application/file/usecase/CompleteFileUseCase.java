package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.dto.response.FileCompleteResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompleteFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    @Transactional
    public FileCompleteResponse execute(UUID fileId) {
        currentAuthProvider.getCurrentAuth();
        return fileService.completeFile(fileId);
    }
}
