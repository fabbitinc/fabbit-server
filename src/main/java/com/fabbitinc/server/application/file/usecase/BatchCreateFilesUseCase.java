package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.file.dto.request.BatchCreateFileRequest;
import com.fabbitinc.server.application.file.dto.response.BatchCreateFileResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BatchCreateFilesUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;

    @Transactional
    public BatchCreateFileResponse execute(String authorizationHeader, BatchCreateFileRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        return fileService.batchCreateFiles(auth, request);
    }
}
