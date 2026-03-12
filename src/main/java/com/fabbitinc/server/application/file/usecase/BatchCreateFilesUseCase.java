package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.file.service.input.CreateFileInput;
import com.fabbitinc.server.application.file.service.output.BatchCreateFilesOutput;
import com.fabbitinc.server.application.file.usecase.command.BatchCreateFilesCommand;
import com.fabbitinc.server.application.file.usecase.result.BatchCreatedFilesResult;
import com.fabbitinc.server.application.file.usecase.result.CreatedFileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class BatchCreateFilesUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;

    public BatchCreatedFilesResult execute(BatchCreateFilesCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        BatchCreateFilesOutput output = fileService.batchCreateFiles(
                auth,
                command.items().stream()
                        .map(item -> new CreateFileInput(
                                item.originalName(),
                                item.contentType(),
                                item.fileSize(),
                                item.contentHash()
                        ))
                        .toList()
        );
        return new BatchCreatedFilesResult(
                output.items().stream()
                        .map(item -> new CreatedFileResult(item.fileId(), item.uploadUrl(), item.fileKey()))
                        .toList()
        );
    }
}
