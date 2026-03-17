package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.query.condition.PartPreviewProcessingCondition;
import com.fabbitinc.server.application.part.query.result.PartPreviewProcessingFailureCode;
import com.fabbitinc.server.application.part.query.result.PartPreviewProcessingResult;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingJob;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingStatus;
import com.fabbitinc.server.domain.part.model.PartPreviewServingProjection;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartPreviewProcessingJobRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewServingProjectionRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartPreviewProcessingQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRevisionRepository partRevisionRepository;
    private final PartPreviewRepository partPreviewRepository;
    private final PartPreviewProcessingJobRepository partPreviewProcessingJobRepository;
    private final PartPreviewServingProjectionRepository partPreviewServingProjectionRepository;

    public PartPreviewProcessingResult get(PartPreviewProcessingCondition condition) {
        currentAuthProvider.getCurrentAuth();
        PartRevision revision = partRevisionRepository.findByIdAndPartId(condition.revisionId(), condition.partId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(condition.partId(), condition.revisionId())
                ));

        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(revision.getId())
                .filter(PartPreview::hasSource)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "대표 미리보기를 찾을 수 없습니다"));

        PartPreviewProcessingJob latestJob = partPreviewProcessingJobRepository
                .findFirstByPartPreviewIdOrderByCreatedAtDesc(partPreview.getId())
                .orElse(null);
        PartPreviewServingProjection projection = partPreviewServingProjectionRepository.findById(partPreview.getId())
                .orElse(null);

        return new PartPreviewProcessingResult(
                partPreview.getSourceType(),
                partPreview.getSourceId(),
                resolveStatus(partPreview, latestJob),
                resolveFailureCode(partPreview, latestJob),
                resolveFailureMessage(partPreview, latestJob),
                hasText(resolvePdfKey(partPreview, projection)),
                hasText(resolveWebpKey(partPreview, projection)),
                hasText(resolveGlbKey(partPreview, projection))
        );
    }

    private PartPreviewProcessingStatus resolveStatus(PartPreview partPreview, PartPreviewProcessingJob latestJob) {
        if (latestJob == null || latestJob.getStatus() == null) {
            return partPreview.getProcessingStatus();
        }
        return mapJobStatus(latestJob.getStatus());
    }

    private PartPreviewProcessingStatus mapJobStatus(DrawingJobStatus jobStatus) {
        return switch (jobStatus) {
            case REQUESTED -> PartPreviewProcessingStatus.PENDING;
            case PROCESSING -> PartPreviewProcessingStatus.PROCESSING;
            case COMPLETED -> PartPreviewProcessingStatus.COMPLETED;
            case FAILED -> PartPreviewProcessingStatus.FAILED;
        };
    }

    private PartPreviewProcessingFailureCode resolveFailureCode(PartPreview partPreview, PartPreviewProcessingJob latestJob) {
        FailureDescriptor failure = resolveFailureDescriptor(partPreview, latestJob);
        return failure == null ? null : failure.code();
    }

    private String resolveFailureMessage(PartPreview partPreview, PartPreviewProcessingJob latestJob) {
        FailureDescriptor failure = resolveFailureDescriptor(partPreview, latestJob);
        return failure == null ? null : failure.message();
    }

    private FailureDescriptor resolveFailureDescriptor(PartPreview partPreview, PartPreviewProcessingJob latestJob) {
        if (partPreview.getProcessingStatus() != PartPreviewProcessingStatus.FAILED || latestJob == null) {
            return null;
        }
        String rawReason = latestJob.getFailureReason();
        if (!hasText(rawReason)) {
            return new FailureDescriptor(
                    PartPreviewProcessingFailureCode.UNKNOWN,
                    "대표 미리보기 처리 중 오류가 발생했습니다."
            );
        }

        if (rawReason.contains("시간이 초과")) {
            return new FailureDescriptor(
                    PartPreviewProcessingFailureCode.TIMEOUT,
                    "대표 미리보기 변환 시간이 초과되었습니다."
            );
        }
        if (rawReason.contains("지원하지 않는 도면 파일 형식")) {
            return new FailureDescriptor(
                    PartPreviewProcessingFailureCode.UNSUPPORTED_FORMAT,
                    "지원하지 않는 미리보기 형식입니다."
            );
        }
        if (rawReason.contains("실행 파일을 찾을 수 없습니다")
                || rawReason.contains("settings.ini 리소스를 찾을 수 없습니다")
                || rawReason.contains("변환기를 사용할 수 없습니다")) {
            return new FailureDescriptor(
                    PartPreviewProcessingFailureCode.CONVERTER_UNAVAILABLE,
                    "대표 미리보기 변환기를 사용할 수 없습니다. 관리자에게 문의해 주세요."
            );
        }
        if (rawReason.contains("결과 파일이 생성되지 않았습니다")
                || rawReason.contains("실행에 실패했습니다")
                || rawReason.contains("실행이 중단되었습니다")
                || rawReason.contains("dwg2pdf 실패")) {
            return new FailureDescriptor(
                    PartPreviewProcessingFailureCode.CONVERSION_FAILED,
                    "대표 미리보기 변환에 실패했습니다."
            );
        }
        return new FailureDescriptor(
                PartPreviewProcessingFailureCode.UNKNOWN,
                "대표 미리보기 처리 중 오류가 발생했습니다."
        );
    }

    private String resolvePdfKey(PartPreview partPreview, PartPreviewServingProjection projection) {
        if (projection != null && hasText(projection.getPdfKey())) {
            return projection.getPdfKey();
        }
        return partPreview.getPdfKey();
    }

    private String resolveWebpKey(PartPreview partPreview, PartPreviewServingProjection projection) {
        if (projection != null && hasText(projection.getWebpKey())) {
            return projection.getWebpKey();
        }
        return partPreview.getWebpKey();
    }

    private String resolveGlbKey(PartPreview partPreview, PartPreviewServingProjection projection) {
        if (projection != null && hasText(projection.getGlbKey())) {
            return projection.getGlbKey();
        }
        return partPreview.getGlbKey();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record FailureDescriptor(
            PartPreviewProcessingFailureCode code,
            String message
    ) {
    }
}
