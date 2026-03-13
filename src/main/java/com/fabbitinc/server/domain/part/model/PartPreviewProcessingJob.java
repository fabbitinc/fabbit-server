package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "part_preview_processing_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartPreviewProcessingJob extends AbstractCreatedEntity {

    @Column(name = "part_preview_id", nullable = false)
    private UUID partPreviewId;

    @Column(name = "pipeline_key", nullable = false, length = 100)
    private String pipelineKey;

    @Column(name = "profile_key", nullable = false, length = 100)
    private String profileKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DrawingJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private PartPreviewProcessingJob(UUID partPreviewId, String pipelineKey, String profileKey) {
        super(UuidV7Generator.next());
        this.partPreviewId = requirePreviewId(partPreviewId);
        this.pipelineKey = requireText(pipelineKey, "PART_PREVIEW_JOB_PIPELINE_REQUIRED", "대표 미리보기 파이프라인 키는 필수입니다");
        this.profileKey = requireText(profileKey, "PART_PREVIEW_JOB_PROFILE_REQUIRED", "대표 미리보기 프로필 키는 필수입니다");
        this.status = DrawingJobStatus.REQUESTED;
        this.attemptCount = 0;
    }

    public static PartPreviewProcessingJob request(UUID partPreviewId, String pipelineKey, String profileKey) {
        return new PartPreviewProcessingJob(partPreviewId, pipelineKey, profileKey);
    }

    public boolean canStart() {
        return status == DrawingJobStatus.REQUESTED;
    }

    public boolean isTerminal() {
        return status == DrawingJobStatus.COMPLETED || status == DrawingJobStatus.FAILED;
    }

    public void start() {
        if (!canStart()) {
            throw new DomainException("PART_PREVIEW_JOB_INVALID_STATE", "요청 상태의 대표 미리보기 작업만 시작할 수 있습니다");
        }
        this.status = DrawingJobStatus.PROCESSING;
        this.attemptCount += 1;
        this.startedAt = Instant.now();
        this.failureReason = null;
    }

    public void complete() {
        this.status = DrawingJobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.failureReason = null;
    }

    public void fail(String reason) {
        this.status = DrawingJobStatus.FAILED;
        this.completedAt = Instant.now();
        this.failureReason = normalizeNullable(reason);
    }

    private UUID requirePreviewId(UUID value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_JOB_PREVIEW_REQUIRED", "대표 미리보기 ID는 필수입니다");
        }
        return value;
    }

    private String requireText(String value, String code, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException(code, message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
