package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DrawingService {

    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final OrganizationApi organizationApi;
    private final PartPreviewService partPreviewService;
    private final DrawingSourceClassifier drawingSourceClassifier;

    public DrawingService(
            DrawingRepository drawingRepository,
            FileRepository fileRepository,
            StoragePort storagePort,
            OrganizationApi organizationApi,
            PartPreviewService partPreviewService
    ) {
        this.drawingRepository = drawingRepository;
        this.fileRepository = fileRepository;
        this.storagePort = storagePort;
        this.organizationApi = organizationApi;
        this.partPreviewService = partPreviewService;
        this.drawingSourceClassifier = new DrawingSourceClassifier();
    }

    public Drawing createDrawing(UUID partRevisionId, UUID fileId) {
        File file = loadConsumableFile(fileId);
        completeUploadIfNeeded(file);
        DrawingSourceDescriptor sourceDescriptor = classifySource(file);

        Drawing drawing = Drawing.create(null, file.getOriginalName());
        drawing.assignPartRevision(partRevisionId);
        drawing.assignSourceFile(file.getId(), sourceDescriptor.sourceType(), sourceDescriptor.dimension());
        drawing.changeOriginalFileKey(file.getFileKey());

        drawingRepository.save(drawing);
        attachFileToDrawing(file, drawing.getId());
        return drawing;
    }

    public void deleteDrawing(UUID drawingId, UUID actorId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));

        Set<String> keys = new LinkedHashSet<>();
        if (drawing.getSourceFileId() != null) {
            fileRepository.findByIdAndDeletedAtIsNull(drawing.getSourceFileId())
                    .map(File::getFileKey)
                    .ifPresent(keys::add);
        }
        if (keys.isEmpty() && drawing.getOriginalFileKey() != null && !drawing.getOriginalFileKey().isBlank()) {
            keys.add(drawing.getOriginalFileKey());
        }
        drawing.getArtifacts().forEach(artifact -> keys.add(artifact.getStorageKey()));
        keys.forEach(this::softDeleteFileByKey);
        drawing.softDelete(actorId);
        partPreviewService.clearByDrawing(drawingId);
    }

    public void deleteDrawing(UUID partRevisionId, UUID drawingId, UUID actorId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .filter(it -> it.getDeletedAt() == null && partRevisionId.equals(it.getPartRevisionId()))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));
        deleteDrawing(drawing.getId(), actorId);
    }

    private void softDeleteFileByKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        fileRepository.findByFileKeyAndDeletedAtIsNull(fileKey)
                .ifPresent(file -> {
                    long fileSize = file.getFileSize();
                    file.softDelete(null);
                    if (fileSize > 0L) {
                        organizationApi.releaseStorageForCurrentTenant(fileSize);
                    }
                });
    }

    private File loadConsumableFile(UUID fileId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));
        if (file.getOwnerId() != null) {
            throw new AppException(ErrorCode.CONFLICT, "이미 다른 리소스에 연결된 파일입니다");
        }
        return file;
    }

    private void completeUploadIfNeeded(File file) {
        if (file.getStatus() == FileStatus.UPLOADED) {
            return;
        }
        if (storagePort.headObject(file.getFileKey()) == null) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "스토리지에 파일이 존재하지 않습니다. 업로드를 완료해주세요."
            );
        }
        file.markUploaded();
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
