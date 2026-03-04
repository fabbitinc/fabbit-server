package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.dto.request.BatchCompleteRequest;
import com.fabbitinc.server.application.file.dto.response.BatchCompleteResponse;
import com.fabbitinc.server.application.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BatchCompleteFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    @Transactional
    public BatchCompleteResponse execute(BatchCompleteRequest request) {
        currentAuthProvider.getCurrentAuth();
        return fileService.batchCompleteFiles(request);
    }
}
