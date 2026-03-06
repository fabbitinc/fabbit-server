package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.file.service.output.FileCompleteOutput;
import com.fabbitinc.server.application.file.usecase.command.CompleteFileCommand;
import com.fabbitinc.server.application.file.usecase.result.CompletedFileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CompleteFileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    public CompletedFileResult execute(CompleteFileCommand command) {
        currentAuthProvider.getCurrentAuth();
        FileCompleteOutput output = fileService.completeFile(command.fileId());
        return new CompletedFileResult(
                output.fileId(),
                output.status(),
                output.originalName(),
                output.fileKey(),
                output.fileSize(),
                output.contentType(),
                output.createdAt()
        );
    }
}
