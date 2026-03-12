package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingRenderSourceGroup;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DrawingService {

    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final DrawingAsyncConversionService drawingAsyncConversionService;
    private final OrganizationApi organizationApi;
    private final DrawingSourceClassifier drawingSourceClassifier;

    public DrawingService(
            DrawingRepository drawingRepository,
            FileRepository fileRepository,
            DrawingAsyncConversionService drawingAsyncConversionService,
            OrganizationApi organizationApi
    ) {
        this.drawingRepository = drawingRepository;
        this.fileRepository = fileRepository;
        this.drawingAsyncConversionService = drawingAsyncConversionService;
        this.organizationApi = organizationApi;
        this.drawingSourceClassifier = new DrawingSourceClassifier();
    }

    public Drawing createDrawing(UUID fileId) {
        File file = loadUploadedFile(fileId);
        DrawingSourceDescriptor sourceDescriptor = classifySource(file);

        Drawing drawing = Drawing.create(null, file.getOriginalName());
        drawing.registerSourceFile(
                file.getId(),
                sourceDescriptor.dimension(),
                file.getFileKey(),
                file.getContentType(),
                file.getFileSize()
        );

        if (sourceDescriptor.extension().requiresRenderSource()) {
            drawing.markRenderSourceRequired();
        } else {
            drawing.registerRenderSourceFile(
                    file.getId(),
                    sourceDescriptor.sourceType(),
                    sourceDescriptor.dimension(),
                    file.getFileKey(),
                    file.getContentType(),
                    file.getFileSize()
            );
            drawing.markConversionPending();
        }

        drawingRepository.save(drawing);
        attachFileToDrawing(file, drawing.getId());

        if (!sourceDescriptor.extension().requiresRenderSource()) {
            dispatchAfterCommit(drawing.getId());
        }
        return drawing;
    }

    public Drawing registerRenderSource(UUID drawingId, UUID fileId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .filter(it -> it.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));

        DrawingRenderSourceGroup requiredGroup = drawing.getExpectedRenderSourceGroup();
        if (requiredGroup == null) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "추가 render source 업로드가 필요하지 않은 도면입니다"
            );
        }

        File file = loadUploadedFile(fileId);
        DrawingSourceDescriptor sourceDescriptor = classifySource(file);
        if (!requiredGroup.supports(sourceDescriptor.extension())) {
            String allowedExtensions = requiredGroup.getAllowedExtensions().stream()
                    .map(extension -> extension.getFormat())
                    .collect(Collectors.joining(", "));
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "허용되지 않는 render source 형식입니다. 허용 형식: " + allowedExtensions
            );
        }

        String previousRenderSourceKey = drawing.getRenderSourceFileKey();
        String originalFileKey = drawing.getOriginalFileKey();

        drawing.registerRenderSourceFile(
                file.getId(),
                sourceDescriptor.sourceType(),
                sourceDescriptor.dimension(),
                file.getFileKey(),
                file.getContentType(),
                file.getFileSize()
        );
        drawing.markConversionPending();
        drawingRepository.save(drawing);

        attachFileToDrawing(file, drawing.getId());
        if (previousRenderSourceKey != null
                && !previousRenderSourceKey.isBlank()
                && !previousRenderSourceKey.equals(originalFileKey)
                && !previousRenderSourceKey.equals(file.getFileKey())) {
            softDeleteFileByKey(previousRenderSourceKey);
        }
        dispatchAfterCommit(drawing.getId());
        return drawing;
    }

    public void deleteDrawing(UUID drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));

        Set<String> keys = new LinkedHashSet<>();
        keys.add(drawing.getOriginalFileKey());
        keys.add(drawing.getPdfKey());
        keys.add(drawing.getThumbnailKey());
        drawing.getArtifacts().forEach(artifact -> keys.add(artifact.getStorageKey()));
        keys.forEach(this::softDeleteFileByKey);
        drawing.softDelete();
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

    private File loadUploadedFile(UUID fileId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));

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

    private void dispatchAfterCommit(UUID drawingId) {
        String schemaName = TenantContextHolder.getCurrentSchema();
        Runnable dispatch = () -> drawingAsyncConversionService.convertDrawingAsync(drawingId, schemaName);

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
}
