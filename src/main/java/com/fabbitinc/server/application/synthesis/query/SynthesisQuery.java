package com.fabbitinc.server.application.synthesis.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.synthesis.query.condition.SynthesisBatchCondition;
import com.fabbitinc.server.application.synthesis.query.condition.SynthesisJobCondition;
import com.fabbitinc.server.application.synthesis.query.condition.SynthesisListCondition;
import com.fabbitinc.server.application.synthesis.query.result.SynthesisBatchStatusResult;
import com.fabbitinc.server.application.synthesis.query.result.SynthesisJobResult;
import com.fabbitinc.server.application.synthesis.query.result.SynthesisListResult;
import com.fabbitinc.server.domain.synthesis.model.SynthesisBatch;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisBatchRepository;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SynthesisQuery {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<BatchFailurePayload>> FAILURE_LIST_TYPE = new TypeReference<>() {
    };

    private final SynthesisBatchRepository synthesisBatchRepository;
    private final SynthesisJobRepository synthesisJobRepository;
    private final ObjectMapper objectMapper;

    public SynthesisBatchStatusResult getBatch(SynthesisBatchCondition condition) {
        UUID batchId = condition.batchId();
        SynthesisBatch batch = synthesisBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "합성 배치를 찾을 수 없습니다"));

        List<SynthesisJob> jobs = synthesisJobRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        int pendingCount = 0;
        int processingCount = 0;
        int completedCount = 0;
        int failedJobCount = 0;
        List<SynthesisBatchStatusResult.SynthesisBatchItemStatusResult> items = new java.util.ArrayList<>();
        for (SynthesisJob job : jobs) {
            switch (job.getStatus()) {
                case PENDING -> pendingCount++;
                case PROCESSING -> processingCount++;
                case FAILED -> failedJobCount++;
                case COMPLETED -> completedCount++;
            }
            items.add(new SynthesisBatchStatusResult.SynthesisBatchItemStatusResult(
                    job.getId(),
                    job.getFileId(),
                    job.getStatus(),
                    job.getTotalRows(),
                    job.getProcessedRows(),
                    job.getNodesCreated(),
                    job.getRelationshipsCreated(),
                    parseErrors(job.getErrors()).size(),
                    job.getStartedAt(),
                    job.getCompletedAt()
            ));
        }

        List<SynthesisBatchStatusResult.SynthesisBatchFailureResult> failed = parseFailures(
                        batch.getFailedUploads()
                ).stream()
                .map(item -> new SynthesisBatchStatusResult.SynthesisBatchFailureResult(
                        item.fileId(),
                        item.reason()
                ))
                .toList();
        int failedCount = failed.size();
        int acceptedCount = batch.getAcceptedCount();
        int doneCount = completedCount + failedJobCount;

        SynthesisBatchStatusResult.Status status;
        if (acceptedCount == 0) {
            status = failedCount > 0
                    ? SynthesisBatchStatusResult.Status.FAILED
                    : SynthesisBatchStatusResult.Status.PENDING;
        } else if (doneCount == acceptedCount) {
            status = failedJobCount == 0
                    ? SynthesisBatchStatusResult.Status.COMPLETED
                    : SynthesisBatchStatusResult.Status.COMPLETED_WITH_ERRORS;
        } else if (processingCount > 0) {
            status = SynthesisBatchStatusResult.Status.PROCESSING;
        } else {
            status = SynthesisBatchStatusResult.Status.PENDING;
        }

        return new SynthesisBatchStatusResult(
                batch.getId(),
                batch.getRequestedCount(),
                batch.getAcceptedCount(),
                failedCount,
                pendingCount,
                processingCount,
                completedCount,
                failedJobCount,
                status,
                failed,
                items,
                batch.getCreatedAt()
        );
    }

    public SynthesisJobResult getJob(SynthesisJobCondition condition) {
        UUID jobId = condition.jobId();
        SynthesisJob job = synthesisJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "합성 작업을 찾을 수 없습니다"));
        return toSynthesisJobResult(job);
    }

    public SynthesisListResult list(SynthesisListCondition condition) {
        List<SynthesisJobResult> items = synthesisJobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSynthesisJobResult)
                .toList();
        return new SynthesisListResult(items);
    }

    private SynthesisJobResult toSynthesisJobResult(SynthesisJob job) {
        return new SynthesisJobResult(
                job.getId(),
                job.getMappingId(),
                job.getFileId(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getNodesCreated(),
                job.getRelationshipsCreated(),
                parseErrors(job.getErrors()),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }

    private List<String> parseErrors(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(raw, STRING_LIST_TYPE);
            if (parsed == null || parsed.isEmpty()) {
                return List.of();
            }
            return parsed.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        } catch (JacksonException ignored) {
            // 기존 개행 문자열 포맷과의 호환을 위해 fallback 처리
        }
        String[] lines = raw.split("\\R");
        List<String> errors = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                errors.add(trimmed);
            }
        }
        return errors;
    }

    private List<BatchFailurePayload> parseFailures(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) {
            return List.of();
        }
        try {
            List<BatchFailurePayload> parsed = objectMapper.readValue(raw, FAILURE_LIST_TYPE);
            return parsed == null ? List.of() : parsed;
        } catch (JacksonException ignored) {
            // 기존 탭 구분 문자열 포맷과의 호환을 위해 fallback 처리
        }
        List<BatchFailurePayload> failures = new ArrayList<>();
        String[] lines = raw.split("\\R");
        for (String line : lines) {
            String[] tokens = line.split("\\t", 2);
            if (tokens.length != 2) {
                continue;
            }
            try {
                failures.add(new BatchFailurePayload(UUID.fromString(tokens[0]), tokens[1]));
            } catch (Exception ignored) {
                // 형식 불일치 항목은 무시
            }
        }
        return failures;
    }

    private record BatchFailurePayload(
            UUID fileId,
            String reason
    ) {
    }
}
