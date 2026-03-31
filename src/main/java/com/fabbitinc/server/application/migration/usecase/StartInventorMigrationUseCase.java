package com.fabbitinc.server.application.migration.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.file.service.input.CreateFileInput;
import com.fabbitinc.server.application.file.service.output.BatchCreateFilesOutput;
import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import com.fabbitinc.server.application.migration.service.InventorMigrationManifestService;
import com.fabbitinc.server.application.migration.service.InventorMigrationSessionService;
import com.fabbitinc.server.application.migration.usecase.result.StartInventorMigrationResult;
import com.fabbitinc.server.application.migration.usecase.command.StartInventorMigrationCommand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class StartInventorMigrationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final InventorMigrationSessionService inventorMigrationSessionService;
    private final InventorMigrationManifestService inventorMigrationManifestService;

    public StartInventorMigrationResult execute(StartInventorMigrationCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        List<InventorManifestFile> files = command.files().stream()
                .map(inventorMigrationManifestService::normalizeFile)
                .toList();
        inventorMigrationManifestService.validateStartInput(command.projectName(), command.ipjPath(), files);

        BatchCreateFilesOutput output = fileService.batchCreateFiles(
                auth,
                files.stream()
                        .map(file -> new CreateFileInput(
                                file.originalName(),
                                file.contentType(),
                                file.sizeBytes(),
                                file.contentHash()
                        ))
                        .toList()
        );

        Map<String, java.util.UUID> manifestPathToFileId = new LinkedHashMap<>();
        List<StartInventorMigrationResult.UploadTargetResult> uploadTargets = java.util.stream.IntStream
                .range(0, files.size())
                .mapToObj(index -> {
                    InventorManifestFile file = files.get(index);
                    var item = output.items().get(index);
                    manifestPathToFileId.put(file.path(), item.fileId());
                    return new StartInventorMigrationResult.UploadTargetResult(
                            file.path(),
                            item.fileId(),
                            item.uploadUrl(),
                            item.fileKey()
                    );
                })
                .toList();

        InventorMigrationSession session = inventorMigrationSessionService.createSession(
                auth,
                command.projectName().trim(),
                command.ipjPath().trim(),
                trimToNull(command.inventorVersion()),
                files,
                manifestPathToFileId
        );

        return new StartInventorMigrationResult(
                session.sessionId(),
                session.projectName(),
                files.size(),
                inventorMigrationManifestService.countImportable(files),
                uploadTargets
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
