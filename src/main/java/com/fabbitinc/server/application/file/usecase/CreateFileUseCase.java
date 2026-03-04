package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.dto.request.CreateFileRequest;
import com.fabbitinc.server.application.file.dto.response.CreateFileResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    @Transactional
    public CreateFileResponse execute(CreateFileRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        return fileService.createFile(auth, request);
    }
}
