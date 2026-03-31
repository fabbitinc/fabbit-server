package com.fabbitinc.server.application.migration.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import com.fabbitinc.server.application.migration.service.InventorMigrationSessionService;
import com.fabbitinc.server.application.migration.service.InventorMigrationValidationService;
import com.fabbitinc.server.application.migration.support.InventorMigrationAnalyzer;
import com.fabbitinc.server.application.migration.usecase.command.PreviewInventorMigrationCommand;
import com.fabbitinc.server.application.migration.usecase.result.PreviewInventorMigrationResult;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreviewInventorMigrationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final InventorMigrationSessionService inventorMigrationSessionService;
    private final InventorMigrationValidationService inventorMigrationValidationService;
    private final InventorMigrationAnalyzer inventorMigrationAnalyzer = new InventorMigrationAnalyzer();

    public PreviewInventorMigrationResult execute(PreviewInventorMigrationCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        InventorMigrationSession session = inventorMigrationSessionService.getAccessibleSession(command.sessionId(), auth);
        InventorMigrationAnalyzer.Analysis analysis = inventorMigrationAnalyzer.analyze(session);
        InventorMigrationValidationService.ValidationResult validation = inventorMigrationValidationService.validate(session, analysis);
        Map<UUID, File> filesById = validation.filesById();

        List<PreviewInventorMigrationResult.ItemResult> items = analysis.items().stream()
                .map(item -> toItemResult(item, filesById, validation))
                .toList();

        List<PreviewInventorMigrationResult.OrphanDrawingResult> orphanDrawings = analysis.orphanDrawings().stream()
                .map(orphan -> {
                    File file = filesById.get(orphan.fileId());
                    boolean uploaded = file != null && file.getStatus() == FileStatus.UPLOADED;
                    return new PreviewInventorMigrationResult.OrphanDrawingResult(
                            orphan.path(),
                            orphan.fileId(),
                            uploaded,
                            "매칭되는 모델 파일이 없어 commit 대상에서 제외됩니다"
                    );
                })
                .toList();

        int warningCount = orphanDrawings.size();
        int errorCount = (int) items.stream().filter(item -> "ERROR".equals(item.status())).count();
        int readyItemCount = (int) items.stream().filter(item -> "READY".equals(item.status())).count();

        return new PreviewInventorMigrationResult(
                session.sessionId(),
                session.projectName(),
                new PreviewInventorMigrationResult.Summary(
                        analysis.totalFileCount(),
                        analysis.importableFileCount(),
                        readyItemCount,
                        warningCount,
                        errorCount
                ),
                items,
                orphanDrawings,
                errorCount == 0
        );
    }

    private PreviewInventorMigrationResult.ItemResult toItemResult(
            InventorMigrationAnalyzer.ImportItem item,
            Map<UUID, File> filesById,
            InventorMigrationValidationService.ValidationResult validation
    ) {
        File modelFile = filesById.get(item.modelFileId());
        boolean modelUploaded = modelFile != null && modelFile.getStatus() == FileStatus.UPLOADED;
        boolean drawingsUploaded = item.matchedDrawingFileIds().stream()
                .map(filesById::get)
                .allMatch(file -> file != null && file.getStatus() == FileStatus.UPLOADED);
        boolean uploaded = modelUploaded && drawingsUploaded;
        String status = "READY";
        String message = null;

        if (validation.duplicatePartNumbersInManifest().contains(item.derivedPartNumber())) {
            status = "ERROR";
            message = "매니페스트 내부에서 중복된 partNumber입니다";
        } else if (validation.duplicatePartNumbersInDatabase().contains(item.derivedPartNumber())) {
            status = "ERROR";
            message = "이미 존재하는 partNumber입니다";
        } else if (!modelUploaded) {
            status = "ERROR";
            message = "모델 파일 업로드가 완료되지 않았습니다";
        } else if (!drawingsUploaded) {
            status = "ERROR";
            message = "매칭된 도면 파일 업로드가 완료되지 않았습니다";
        }

        return new PreviewInventorMigrationResult.ItemResult(
                item.path(),
                item.fileType().name(),
                item.derivedPartNumber(),
                item.modelFileId(),
                uploaded,
                status,
                message,
                item.matchedDrawingFileIds(),
                item.matchedDrawingPaths()
        );
    }
}
