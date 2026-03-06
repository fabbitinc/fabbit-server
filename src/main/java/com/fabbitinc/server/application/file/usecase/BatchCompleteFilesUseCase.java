package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.file.service.output.BatchCompleteFilesOutput;
import com.fabbitinc.server.application.file.usecase.command.BatchCompleteFilesCommand;
import com.fabbitinc.server.application.file.usecase.result.BatchCompleteFailureResult;
import com.fabbitinc.server.application.file.usecase.result.BatchCompletedFilesResult;
import com.fabbitinc.server.application.file.usecase.result.CompletedFileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class BatchCompleteFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    public BatchCompletedFilesResult execute(BatchCompleteFilesCommand command) {
        currentAuthProvider.getCurrentAuth();
        BatchCompleteFilesOutput output = fileService.completeFiles(command.fileIds());
        return new BatchCompletedFilesResult(
                output.items().stream()
                        .map(item -> new CompletedFileResult(
                                item.fileId(),
                                item.status(),
                                item.originalName(),
                                item.fileKey(),
                                item.fileSize(),
                                item.contentType(),
                                item.createdAt()
                        ))
                        .toList(),
                output.failed().stream()
                        .map(item -> new BatchCompleteFailureResult(item.fileId(), item.reason()))
                        .toList()
        );
    }
}
