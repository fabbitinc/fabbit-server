package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.drawing.service.DrawingSourceClassifier;
import com.fabbitinc.server.application.drawing.service.DrawingSourceDescriptor;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewFile;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import java.util.UUID;
import java.util.List;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartPreviewService {

    public static final String OWNER_TYPE_PREVIEW_FILE = "part_preview_file";

    private final PartRevisionRepository partRevisionRepository;
    private final PartPreviewRepository partPreviewRepository;
    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final OrganizationApi organizationApi;
    private final PartPreviewArtifactService partPreviewArtifactService;
    private final PartPreviewArtifactCleanupService partPreviewArtifactCleanupService;
    private final PartPreviewAsyncConversionService partPreviewAsyncConversionService;
    private final PartPreviewServingProjectionService partPreviewServingProjectionService;
    private final DrawingSourceClassifier drawingSourceClassifier;

    public PartPreview changeSource(UUID partRevisionId, PartPreviewSourceType sourceType, UUID sourceId) {
        PartRevision revision = getRequiredRevision(partRevisionId);
        PartPreview partPreview = getOrCreatePartPreview(partRevisionId);
        return changeSource(partPreview, revision, sourceType, sourceId);
    }

    public PartPreview uploadPreviewFile(UUID partRevisionId, UUID fileId) {
        PartRevision revision = getRequiredRevision(partRevisionId);
        PartPreview partPreview = getOrCreatePartPreview(partRevisionId);
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));

        if (file.getOwnerId() != null) {
            throw new AppException(ErrorCode.CONFLICT, "이미 다른 리소스에 연결된 파일입니다");
        }
        validatePreviewable(file);

        PartPreviewFile previewFile = partPreview.addPreviewFile(file.getId());
        file.assignOwner(OWNER_TYPE_PREVIEW_FILE, previewFile.getId());
        PartPreview updatedPreview = changeSource(partPreview, revision, PartPreviewSourceType.PREVIEW_FILE, previewFile.getId());
        if (file.getFileSize() > 0L) {
            organizationApi.consumeStorageForCurrentTenant(file.getFileSize());
        }
        return updatedPreview;
    }

    private PartPreview changeSource(
            PartPreview partPreview,
            PartRevision revision,
            PartPreviewSourceType sourceType,
            UUID sourceId
    ) {
        ResolvedSource resolvedSource = resolveSource(partPreview, revision, sourceType, sourceId);
        List<String> generatedArtifactKeys = collectGeneratedArtifactKeys(partPreview);
        partPreview.removeDerivedArtifacts();
        partPreview.replaceSource(sourceType, sourceId, resolvedSource.sourceDescriptor().dimension());
        partPreview.registerSourceFile(
                resolvedSource.file().getId(),
                resolvedSource.file().getFileKey(),
                resolvedSource.file().getContentType(),
                resolvedSource.file().getFileSize()
        );
        partPreviewRepository.save(partPreview);
        partPreviewServingProjectionService.upsert(partPreview);
        dispatchAfterCommit(partPreview.getId(), generatedArtifactKeys);
        return partPreview;
    }

    public void clearByPartRevision(UUID partRevisionId) {
        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(partRevisionId).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            return;
        }
        List<String> generatedArtifactKeys = collectGeneratedArtifactKeys(partPreview);
        partPreview.clearSource();
        partPreviewRepository.save(partPreview);
        partPreviewServingProjectionService.upsert(partPreview);
        dispatchCleanupAfterCommit(generatedArtifactKeys);
    }

    public void clearByDrawing(UUID drawingId) {
        partPreviewRepository.findBySourceTypeAndSourceId(PartPreviewSourceType.DRAWING, drawingId)
                .forEach(this::clearPreview);
    }

    public void deletePreviewFile(UUID partRevisionId, UUID previewFileId, UUID actorId) {
        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(partRevisionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "대표 미리보기를 찾을 수 없습니다"));

        PartPreviewFile previewFile = partPreview.getPreviewFiles().stream()
                .filter(it -> it.getId().equals(previewFileId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "미리보기 전용 파일을 찾을 수 없습니다"));
        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                        previewFile.getFileId(),
                        OWNER_TYPE_PREVIEW_FILE,
                        previewFile.getId()
                )
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "미리보기 전용 파일을 찾을 수 없습니다"));

        if (partPreview.getSourceType() == PartPreviewSourceType.PREVIEW_FILE
                && previewFile.getId().equals(partPreview.getSourceId())) {
            clearPreview(partPreview);
        }

        partPreview.removePreviewFile(previewFile.getId());
        long fileSize = file.getFileSize();
        file.softDelete(actorId);
        if (fileSize > 0L) {
            organizationApi.releaseStorageForCurrentTenant(fileSize);
        }
    }

    private void clearPreview(PartPreview partPreview) {
        if (!partPreview.hasSource()) {
            return;
        }
        List<String> generatedArtifactKeys = collectGeneratedArtifactKeys(partPreview);
        partPreview.clearSource();
        partPreviewRepository.save(partPreview);
        partPreviewServingProjectionService.upsert(partPreview);
        dispatchCleanupAfterCommit(generatedArtifactKeys);
    }

    private PartRevision getRequiredRevision(UUID partRevisionId) {
        return partRevisionRepository.findById(partRevisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '" + partRevisionId + "'을(를) 찾을 수 없습니다"
                ));
    }

    private PartPreview getOrCreatePartPreview(UUID partRevisionId) {
        return partPreviewRepository.findByPartRevisionId(partRevisionId)
                .orElseGet(() -> PartPreview.create(partRevisionId));
    }

    private ResolvedSource resolveSource(
            PartPreview partPreview,
            PartRevision revision,
            PartPreviewSourceType sourceType,
            UUID sourceId
    ) {
        if (sourceType == PartPreviewSourceType.DRAWING) {
            Drawing drawing = drawingRepository.findById(sourceId)
                    .filter(it -> it.getDeletedAt() == null && revision.getId().equals(it.getPartRevisionId()))
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "PartRevision '%s/%s'에 연결된 도면 '%s'을(를) 찾을 수 없습니다"
                                    .formatted(revision.getPartNumber(), revision.getRevisionCode(), sourceId)
                    ));
            if (drawing.getSourceFileId() == null) {
                throw new AppException(ErrorCode.INVALID_STATE, "도면 원본 파일이 없습니다");
            }
            File file = fileRepository.findByIdAndDeletedAtIsNull(drawing.getSourceFileId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면 원본 파일을 찾을 수 없습니다"));
            return new ResolvedSource(file, validatePreviewable(file));
        }

        if (sourceType == PartPreviewSourceType.PREVIEW_FILE) {
            PartPreviewFile previewFile = partPreview.getPreviewFiles().stream()
                    .filter(it -> it.getId().equals(sourceId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "대표 미리보기 전용 파일 '" + sourceId + "'을(를) 찾을 수 없습니다"
                    ));
            File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                            previewFile.getFileId(),
                            OWNER_TYPE_PREVIEW_FILE,
                            previewFile.getId()
                    )
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "대표 미리보기 전용 파일의 원본을 찾을 수 없습니다"
                    ));
            return new ResolvedSource(file, validatePreviewable(file));
        }

        throw new AppException(
                ErrorCode.VALIDATION_ERROR,
                "대표 미리보기는 도면 또는 미리보기 전용 파일만 선택할 수 있습니다"
        );
    }

    private DrawingSourceDescriptor validatePreviewable(File file) {
        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "업로드가 완료되지 않은 파일입니다");
        }
        DrawingSourceDescriptor sourceDescriptor;
        try {
            sourceDescriptor = drawingSourceClassifier.classify(file.getOriginalName());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        if (!sourceDescriptor.extension().canStartPipelineDirectly()) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "대표 미리보기는 직접 변환 가능한 파일만 선택할 수 있습니다"
            );
        }
        return sourceDescriptor;
    }

    private List<String> collectGeneratedArtifactKeys(PartPreview partPreview) {
        List<UUID> artifactFileIds = partPreview.getArtifacts().stream()
                .filter(artifact -> artifact.getArtifactType() != DrawingArtifactType.SOURCE_ORIGINAL)
                .map(artifact -> artifact.getFileId())
                .filter(fileId -> fileId != null)
                .distinct()
                .toList();
        if (artifactFileIds.isEmpty()) {
            return List.of();
        }

        return fileRepository.findByIdIn(artifactFileIds).stream()
                .filter(file -> file.getDeletedAt() == null)
                .filter(file -> PartPreviewArtifactService.OWNER_TYPE.equals(file.getOwnerType()))
                .filter(file -> partPreview.getId().equals(file.getOwnerId()))
                .map(File::getFileKey)
                .filter(storageKey -> storageKey != null && !storageKey.isBlank())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf
                ));
    }

    private void dispatchAfterCommit(UUID partPreviewId, List<String> generatedArtifactKeys) {
        String schemaName = TenantContextHolder.getCurrentSchema();
        Runnable dispatch = () -> {
            try {
                partPreviewArtifactCleanupService.cleanupArtifactFiles(generatedArtifactKeys);
            } catch (Exception ex) {
                log.error(
                        "event=part_preview_cleanup_after_commit_failed part_preview_id={} generated_artifact_count={} reason={}",
                        partPreviewId,
                        generatedArtifactKeys.size(),
                        ex.getMessage(),
                        ex
                );
            }
            log.info(
                    "event=part_preview_conversion_dispatched part_preview_id={} generated_artifact_count={}",
                    partPreviewId,
                    generatedArtifactKeys.size()
            );
            partPreviewAsyncConversionService.convertPartPreviewAsync(partPreviewId, schemaName);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }

        dispatch.run();
    }

    private void dispatchCleanupAfterCommit(List<String> generatedArtifactKeys) {
        Runnable dispatch = () -> {
            try {
                partPreviewArtifactCleanupService.cleanupArtifactFiles(generatedArtifactKeys);
            } catch (Exception ex) {
                log.error(
                        "event=part_preview_cleanup_only_after_commit_failed generated_artifact_count={} reason={}",
                        generatedArtifactKeys.size(),
                        ex.getMessage(),
                        ex
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
            return;
        }

        dispatch.run();
    }

    private record ResolvedSource(
            File file,
            DrawingSourceDescriptor sourceDescriptor
    ) {
    }
}
