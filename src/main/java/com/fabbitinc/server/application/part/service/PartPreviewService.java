package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.drawing.service.DrawingSourceClassifier;
import com.fabbitinc.server.application.drawing.service.DrawingSourceDescriptor;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PartPreviewService {

    private final PartRevisionRepository partRevisionRepository;
    private final PartPreviewRepository partPreviewRepository;
    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final PartPreviewArtifactService partPreviewArtifactService;
    private final PartPreviewAsyncConversionService partPreviewAsyncConversionService;
    private final DrawingSourceClassifier drawingSourceClassifier;

    public PartPreview changeSource(UUID partRevisionId, PartPreviewSourceType sourceType, UUID sourceId) {
        PartRevision revision = getRequiredRevision(partRevisionId);
        ResolvedSource resolvedSource = resolveSource(revision, sourceType, sourceId);

        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(partRevisionId)
                .orElseGet(() -> PartPreview.create(partRevisionId));
        partPreviewArtifactService.cleanupPreviewArtifacts(partPreview);
        partPreview.replaceSource(sourceType, sourceId, resolvedSource.sourceDescriptor().dimension());
        partPreview.registerSourceFile(
                resolvedSource.file().getId(),
                resolvedSource.file().getFileKey(),
                resolvedSource.file().getContentType(),
                resolvedSource.file().getFileSize()
        );
        partPreviewRepository.save(partPreview);
        dispatchAfterCommit(partPreview.getId());
        return partPreview;
    }

    public void clearByPartRevision(UUID partRevisionId) {
        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(partRevisionId).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            return;
        }
        partPreviewArtifactService.cleanupPreviewArtifacts(partPreview);
        partPreview.clearSource();
        partPreviewRepository.save(partPreview);
    }

    public void clearByFile(UUID fileId) {
        partPreviewRepository.findBySourceTypeAndSourceId(PartPreviewSourceType.FILE, fileId)
                .forEach(this::clearPreview);
    }

    public void clearByDrawing(UUID drawingId) {
        partPreviewRepository.findBySourceTypeAndSourceId(PartPreviewSourceType.DRAWING, drawingId)
                .forEach(this::clearPreview);
    }

    private void clearPreview(PartPreview partPreview) {
        if (!partPreview.hasSource()) {
            return;
        }
        partPreviewArtifactService.cleanupPreviewArtifacts(partPreview);
        partPreview.clearSource();
        partPreviewRepository.save(partPreview);
    }

    private PartRevision getRequiredRevision(UUID partRevisionId) {
        return partRevisionRepository.findById(partRevisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '" + partRevisionId + "'을(를) 찾을 수 없습니다"
                ));
    }

    private ResolvedSource resolveSource(PartRevision revision, PartPreviewSourceType sourceType, UUID sourceId) {
        if (sourceType == PartPreviewSourceType.FILE) {
            File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(sourceId, "part_revision", revision.getId())
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "PartRevision '" + revision.getId() + "'에 연결된 파일 '" + sourceId + "'을(를) 찾을 수 없습니다"
                    ));
            return new ResolvedSource(file, validatePreviewable(file));
        }

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

        throw new AppException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 대표 미리보기 소스 타입입니다");
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

    private void dispatchAfterCommit(UUID partPreviewId) {
        String schemaName = TenantContextHolder.getCurrentSchema();
        Runnable dispatch = () -> partPreviewAsyncConversionService.convertPartPreviewAsync(partPreviewId, schemaName);

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
