package com.fabbitinc.server.application.drawing.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.drawing.query.condition.DrawingProcessingCondition;
import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingFailureCode;
import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingResult;
import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingActionRequiredReason;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingProcessingJob;
import com.fabbitinc.server.domain.drawing.model.DrawingServingProjection;
import com.fabbitinc.server.domain.drawing.repository.DrawingProcessingJobRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingServingProjectionRepository;
import java.util.List;
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
        DrawingProcessingStatus status = resolveStatus(drawing, latestJob);

        return new DrawingProcessingResult(
                status,
                resolveFailureCode(drawing, latestJob),
                resolveFailureMessage(drawing, latestJob),
                hasText(resolvePdfKey(drawing, projection)),
                hasText(resolveWebpKey(drawing, projection)),
                hasText(resolveGlbKey(drawing, projection)),
                resolveActionRequiredReason(status),
                resolveAllowedRenderSourceExtensions(drawing)
        );
    }

    private DrawingProcessingStatus resolveStatus(Drawing drawing, DrawingProcessingJob latestJob) {
        if (drawing.isRenderSourceRequired() || drawing.getConversionStatus() == DrawingConversionStatus.ACTION_REQUIRED) {
            return DrawingProcessingStatus.ACTION_REQUIRED;
        }
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

    private DrawingActionRequiredReason resolveActionRequiredReason(DrawingProcessingStatus status) {
        if (status != DrawingProcessingStatus.ACTION_REQUIRED) {
            return null;
        }
        return DrawingActionRequiredReason.RENDER_SOURCE_REQUIRED;
    }

    private List<String> resolveAllowedRenderSourceExtensions(Drawing drawing) {
        if (drawing.getConversionStatus() != DrawingConversionStatus.ACTION_REQUIRED
                || drawing.getExpectedRenderSourceGroup() == null) {
            return List.of();
        }
        return drawing.getExpectedRenderSourceGroup().getAllowedFormats();
    }

    private DrawingProcessingFailureCode resolveFailureCode(Drawing drawing, DrawingProcessingJob latestJob) {
        FailureDescriptor failure = resolveFailureDescriptor(drawing, latestJob);
        if (failure == null) {
            return null;
        }
        return failure.code();
    }

    private String resolveFailureMessage(Drawing drawing, DrawingProcessingJob latestJob) {
        FailureDescriptor failure = resolveFailureDescriptor(drawing, latestJob);
        if (failure == null) {
            return null;
        }
        return failure.message();
    }

    private FailureDescriptor resolveFailureDescriptor(Drawing drawing, DrawingProcessingJob latestJob) {
        if (drawing.getConversionStatus() != DrawingConversionStatus.FAILED || latestJob == null) {
            return null;
        }
        String rawReason = latestJob.getFailureReason();
        if (!hasText(rawReason)) {
            return new FailureDescriptor(
                    DrawingProcessingFailureCode.UNKNOWN,
                    "도면 처리 중 오류가 발생했습니다."
            );
        }

        if (rawReason.contains("시간이 초과")) {
            return new FailureDescriptor(
                    DrawingProcessingFailureCode.TIMEOUT,
                    "도면 변환 시간이 초과되었습니다."
            );
        }
        if (rawReason.contains("지원하지 않는 도면 파일 형식")) {
            return new FailureDescriptor(
                    DrawingProcessingFailureCode.UNSUPPORTED_FORMAT,
                    "지원하지 않는 도면 형식입니다."
            );
        }
        if (rawReason.contains("실행 파일을 찾을 수 없습니다")
                || rawReason.contains("settings.ini 리소스를 찾을 수 없습니다")
                || rawReason.contains("변환기를 사용할 수 없습니다")) {
            return new FailureDescriptor(
                    DrawingProcessingFailureCode.CONVERTER_UNAVAILABLE,
                    "도면 변환기를 사용할 수 없습니다. 관리자에게 문의해 주세요."
            );
        }
        if (rawReason.contains("결과 파일이 생성되지 않았습니다")
                || rawReason.contains("실행에 실패했습니다")
                || rawReason.contains("실행이 중단되었습니다")
                || rawReason.contains("dwg2pdf 실패")) {
            return new FailureDescriptor(
                    DrawingProcessingFailureCode.CONVERSION_FAILED,
                    "도면 변환에 실패했습니다."
            );
        }
        return new FailureDescriptor(
                DrawingProcessingFailureCode.UNKNOWN,
                "도면 처리 중 오류가 발생했습니다."
        );
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

    private record FailureDescriptor(
            DrawingProcessingFailureCode code,
            String message
    ) {
    }
}
