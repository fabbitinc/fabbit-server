package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DrawingService {

    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final OrganizationApi organizationApi;
    private final PartPreviewService partPreviewService;
    private final DrawingSourceClassifier drawingSourceClassifier;

    public DrawingService(
            DrawingRepository drawingRepository,
            FileRepository fileRepository,
            OrganizationApi organizationApi,
            PartPreviewService partPreviewService
    ) {
        this.drawingRepository = drawingRepository;
        this.fileRepository = fileRepository;
        this.organizationApi = organizationApi;
        this.partPreviewService = partPreviewService;
        this.drawingSourceClassifier = new DrawingSourceClassifier();
    }

    public Drawing createDrawing(UUID partRevisionId, UUID fileId) {
        File file = loadUploadedFile(partRevisionId, fileId);
        DrawingSourceDescriptor sourceDescriptor = classifySource(file);

        Drawing drawing = Drawing.create(null, file.getOriginalName());
        drawing.assignPartRevision(partRevisionId);
        drawing.registerSourceFile(
                file.getId(),
                sourceDescriptor.dimension(),
                file.getFileKey(),
                file.getContentType(),
                file.getFileSize()
        );
        drawing.assignSourceFile(file.getId(), sourceDescriptor.sourceType(), sourceDescriptor.dimension());

        drawingRepository.save(drawing);
        attachFileToDrawing(file, drawing.getId());
        return drawing;
    }

    public void deleteDrawing(UUID drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));

        Set<String> keys = new LinkedHashSet<>();
        drawing.getArtifacts().forEach(artifact -> keys.add(artifact.getStorageKey()));
        keys.forEach(this::softDeleteFileByKey);
        drawing.softDelete();
        partPreviewService.clearByDrawing(drawingId);
    }

    public void deleteDrawing(UUID partRevisionId, UUID drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .filter(it -> it.getDeletedAt() == null && partRevisionId.equals(it.getPartRevisionId()))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));
        deleteDrawing(drawing.getId());
    }

    private void softDeleteFileByKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        fileRepository.findByFileKeyAndDeletedAtIsNull(fileKey)
                .ifPresent(file -> {
                    long fileSize = file.getFileSize();
                    file.softDelete();
                    if (fileSize > 0L) {
                        organizationApi.releaseStorageForCurrentTenant(fileSize);
                    }
                });
    }

    private File loadUploadedFile(UUID partRevisionId, UUID fileId) {
        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(fileId, "part_revision", partRevisionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "리비전에 연결된 파일을 찾을 수 없습니다"));

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "업로드가 완료되지 않은 파일입니다"
            );
        }
        return file;
    }

    private DrawingSourceDescriptor classifySource(File file) {
        try {
            return drawingSourceClassifier.classify(file.getOriginalName());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    private void attachFileToDrawing(File file, UUID drawingId) {
        file.assignOwner("drawing", drawingId);
        if (file.getFileSize() > 0L) {
            organizationApi.consumeStorageForCurrentTenant(file.getFileSize());
        }
    }
}
