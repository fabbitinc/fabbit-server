package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.file.service.FileCleanupService;
import com.fabbitinc.server.application.file.usecase.command.CleanupExpiredDeletedFilesCommand;
import com.fabbitinc.server.application.file.usecase.result.CleanupExpiredDeletedFilesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CleanupExpiredDeletedFilesUseCase {

    private final FileCleanupService fileCleanupService;

    public CleanupExpiredDeletedFilesResult execute(CleanupExpiredDeletedFilesCommand command) {
        int deletedCount = fileCleanupService.cleanupExpiredDeletedFiles(
                command.retention(),
                command.batchSize()
        );
        return new CleanupExpiredDeletedFilesResult(deletedCount);
    }
}
