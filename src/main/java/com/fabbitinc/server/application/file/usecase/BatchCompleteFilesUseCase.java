package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.file.dto.request.BatchCompleteRequest;
import com.fabbitinc.server.application.file.dto.response.BatchCompleteResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BatchCompleteFilesUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;

    @Transactional
    public BatchCompleteResponse execute(String authorizationHeader, BatchCompleteRequest request) {
        authTokenParser.requireAuth(authorizationHeader);
        return fileService.batchCompleteFiles(request);
    }
}
