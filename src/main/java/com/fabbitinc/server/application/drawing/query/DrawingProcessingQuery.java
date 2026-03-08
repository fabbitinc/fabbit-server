package com.fabbitinc.server.application.drawing.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.drawing.query.condition.DrawingProcessingCondition;
import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingResult;
import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingStatus;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingProcessingJob;
import com.fabbitinc.server.domain.drawing.model.DrawingServingProjection;
import com.fabbitinc.server.domain.drawing.repository.DrawingProcessingJobRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingServingProjectionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrawingProcessingQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final DrawingRepository drawingRepository;
    private final DrawingProcessingJobRepository drawingProcessingJobRepository;
    private final DrawingServingProjectionRepository drawingServingProjectionRepository;

    public DrawingProcessingResult get(DrawingProcessingCondition condition) {
        currentAuthProvider.getCurrentAuth();

        UUID drawingId = condition.drawingId();
        Drawing drawing = drawingRepository.findById(drawingId)
                .filter(it -> it.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "도면을 찾을 수 없습니다"));

        DrawingProcessingJob latestJob = drawingProcessingJobRepository
                .findFirstByDrawingIdOrderByCreatedAtDesc(drawingId)
                .orElse(null);
        DrawingServingProjection projection = drawingServingProjectionRepository.findById(drawingId).orElse(null);

        return new DrawingProcessingResult(
                resolveStatus(drawing, latestJob),
                resolveFailureReason(drawing, latestJob),
                hasText(resolvePdfKey(drawing, projection)),
                hasText(resolveWebpKey(drawing, projection)),
                hasText(resolveGlbKey(drawing, projection))
        );
    }

    private DrawingProcessingStatus resolveStatus(Drawing drawing, DrawingProcessingJob latestJob) {
        if (drawing.getConversionStatus() == DrawingConversionStatus.COMPLETED) {
            return DrawingProcessingStatus.COMPLETED;
        }
        if (drawing.getConversionStatus() == DrawingConversionStatus.FAILED) {
            return DrawingProcessingStatus.FAILED;
        }
        if (latestJob == null) {
            return DrawingProcessingStatus.PENDING;
        }
        return mapJobStatus(latestJob.getStatus());
    }

    private DrawingProcessingStatus mapJobStatus(DrawingJobStatus jobStatus) {
        if (jobStatus == null) {
            return DrawingProcessingStatus.PENDING;
        }
        return switch (jobStatus) {
            case REQUESTED -> DrawingProcessingStatus.PENDING;
            case PROCESSING -> DrawingProcessingStatus.PROCESSING;
            case COMPLETED -> DrawingProcessingStatus.COMPLETED;
            case FAILED -> DrawingProcessingStatus.FAILED;
        };
    }

    private String resolveFailureReason(Drawing drawing, DrawingProcessingJob latestJob) {
        if (drawing.getConversionStatus() != DrawingConversionStatus.FAILED || latestJob == null) {
            return null;
        }
        return latestJob.getFailureReason();
    }

    private String resolvePdfKey(Drawing drawing, DrawingServingProjection projection) {
        if (projection != null && hasText(projection.getPdfKey())) {
            return projection.getPdfKey();
        }
        return drawing.getPdfKey();
    }

    private String resolveWebpKey(Drawing drawing, DrawingServingProjection projection) {
        if (projection != null && hasText(projection.getWebpKey())) {
            return projection.getWebpKey();
        }
        return drawing.getWebpKey();
    }

    private String resolveGlbKey(Drawing drawing, DrawingServingProjection projection) {
        if (projection != null && hasText(projection.getGlbKey())) {
            return projection.getGlbKey();
        }
        return drawing.getGlbKey();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
