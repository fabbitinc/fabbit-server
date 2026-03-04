package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.file.dto.request.CreateFileRequest;
import com.fabbitinc.server.application.file.dto.response.CreateFileResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateFileUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;

    @Transactional
    public CreateFileResponse execute(String authorizationHeader, CreateFileRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        return fileService.createFile(auth, request);
    }
}
