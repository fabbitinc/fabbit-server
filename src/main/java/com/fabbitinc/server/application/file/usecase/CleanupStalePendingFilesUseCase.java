package com.fabbitinc.server.application.file.usecase;

import com.fabbitinc.server.application.file.service.FileCleanupService;
import com.fabbitinc.server.application.file.usecase.command.CleanupStalePendingFilesCommand;
import com.fabbitinc.server.application.file.usecase.result.CleanupStalePendingFilesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CleanupStalePendingFilesUseCase {

    private final FileCleanupService fileCleanupService;

    public CleanupStalePendingFilesResult execute(CleanupStalePendingFilesCommand command) {
        int deletedCount = fileCleanupService.cleanupStalePendingFiles(
                command.maxAge(),
                command.batchSize()
        );
        return new CleanupStalePendingFilesResult(deletedCount);
    }
}
