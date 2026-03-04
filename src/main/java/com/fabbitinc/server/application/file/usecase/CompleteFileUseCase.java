package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.file.dto.response.FileCompleteResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompleteFileUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;

    @Transactional
    public FileCompleteResponse execute(String authorizationHeader, UUID fileId) {
        authTokenParser.requireAuth(authorizationHeader);
        return fileService.completeFile(fileId);
    }
}
