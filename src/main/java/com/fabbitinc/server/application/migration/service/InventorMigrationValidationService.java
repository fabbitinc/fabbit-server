package com.fabbitinc.server.application.migration.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import com.fabbitinc.server.application.migration.support.InventorMigrationAnalyzer;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InventorMigrationValidationService {

    private final FileRepository fileRepository;
    private final PartRepository partRepository;

    public InventorMigrationValidationService(
            FileRepository fileRepository,
            PartRepository partRepository
    ) {
        this.fileRepository = fileRepository;
        this.partRepository = partRepository;
    }

    public ValidationResult validate(InventorMigrationSession session, InventorMigrationAnalyzer.Analysis analysis) {
        Map<UUID, File> filesById = fileRepository.findByIdIn(session.fileIds()).stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));

        Set<String> duplicateInManifest = new LinkedHashSet<>(analysis.duplicatePartNumbers());
        Set<String> duplicateInDatabase = partRepository.findByPartNumberIn(
                        analysis.items().stream().map(InventorMigrationAnalyzer.ImportItem::derivedPartNumber).toList()
                ).stream()
                .map(part -> part.getPartNumber())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ValidationResult(filesById, duplicateInManifest, duplicateInDatabase);
    }

    public void validateCommitReady(InventorMigrationSession session, InventorMigrationAnalyzer.Analysis analysis) {
        ValidationResult result = validate(session, analysis);
        if (!result.duplicatePartNumbersInManifest().isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT, "매니페스트 내부 중복 품번이 있습니다: " + result.duplicatePartNumbersInManifest());
        }
        if (!result.duplicatePartNumbersInDatabase().isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 품번이 있습니다: " + result.duplicatePartNumbersInDatabase());
        }

        List<UUID> missingUploads = analysis.items().stream()
                .map(InventorMigrationAnalyzer.ImportItem::modelFileId)
                .filter(fileId -> {
                    File file = result.filesById().get(fileId);
                    return file == null || file.getStatus() != FileStatus.UPLOADED;
                })
                .toList();
        if (!missingUploads.isEmpty()) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "업로드가 완료되지 않은 모델 파일이 있습니다: " + missingUploads);
        }

        List<UUID> drawingUploadFailures = analysis.items().stream()
                .flatMap(item -> item.matchedDrawingFileIds().stream())
                .filter(fileId -> {
                    File file = result.filesById().get(fileId);
                    return file == null || file.getStatus() != FileStatus.UPLOADED;
                })
                .distinct()
                .toList();
        if (!drawingUploadFailures.isEmpty()) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "업로드가 완료되지 않은 도면 파일이 있습니다: " + drawingUploadFailures);
        }
    }

    public record ValidationResult(
            Map<UUID, File> filesById,
            Set<String> duplicatePartNumbersInManifest,
            Set<String> duplicatePartNumbersInDatabase
    ) {
    }
}
