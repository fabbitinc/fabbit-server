package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DrawingService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".dwg",
            ".dxf",
            ".pdf",
            ".png",
            ".jpg",
            ".jpeg",
            ".tif",
            ".tiff"
    );

    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;

    public Drawing createDrawing(UUID fileId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "업로드가 완료되지 않은 파일입니다"
            );
        }

        String extension = extractExtension(file.getOriginalName());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "도면으로 등록할 수 없는 파일 형식입니다: " + extension
            );
        }

        Drawing drawing = Drawing.create(null, file.getOriginalName());
        drawing.setOriginalFileKey(file.getFileKey());
        drawing.markConversionPending();
        drawingRepository.save(drawing);

        file.assignOwner("drawing", drawing.getId());
        return drawing;
    }

    public void deleteDrawing(UUID drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));

        softDeleteFileByKey(drawing.getOriginalFileKey());
        softDeleteFileByKey(drawing.getPdfKey());
        softDeleteFileByKey(drawing.getThumbnailKey());
        drawing.softDelete();
    }

    private void softDeleteFileByKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }
        fileRepository.findByFileKeyAndDeletedAtIsNull(fileKey)
                .ifPresent(File::softDelete);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx);
    }
}
