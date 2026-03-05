package com.fabbitinc.server.application.synthesis.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchFailure;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchItemStatus;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStatus;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStatusResponse;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisJobResponse;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisListResponse;
import com.fabbitinc.server.application.synthesis.support.SynthesisResponseMapper;
import com.fabbitinc.server.domain.synthesis.model.SynthesisBatch;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisBatchRepository;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SynthesisQuery {

    private final SynthesisBatchRepository synthesisBatchRepository;
    private final SynthesisJobRepository synthesisJobRepository;
    private final SynthesisResponseMapper synthesisResponseMapper;

    @Transactional(readOnly = true)
    public SynthesisBatchStatusResponse getSynthesisBatch(UUID batchId) {
        SynthesisBatch batch = synthesisBatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "합성 배치를 찾을 수 없습니다"));

        List<SynthesisJob> jobs = synthesisJobRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        int pendingCount = 0;
        int processingCount = 0;
        int completedCount = 0;
        int failedJobCount = 0;
        List<SynthesisBatchItemStatus> items = new java.util.ArrayList<>();
        for (SynthesisJob job : jobs) {
            switch (job.getStatus()) {
                case PENDING -> pendingCount++;
                case PROCESSING -> processingCount++;
                case FAILED -> failedJobCount++;
                case COMPLETED -> completedCount++;
            }
            items.add(new SynthesisBatchItemStatus(
                    job.getId(),
                    job.getFileId(),
                    job.getStatus(),
                    job.getTotalRows(),
                    job.getProcessedRows(),
                    job.getNodesCreated(),
                    job.getRelationshipsCreated(),
                    synthesisResponseMapper.parseErrors(job.getErrors()).size(),
                    job.getStartedAt(),
                    job.getCompletedAt()
            ));
        }

        List<SynthesisBatchFailure> failed = synthesisResponseMapper.parseFailures(batch.getFailedUploads());
        int failedCount = failed.size();
        int acceptedCount = batch.getAcceptedCount();
        int doneCount = completedCount + failedJobCount;

        SynthesisBatchStatus status;
        if (acceptedCount == 0) {
            status = failedCount > 0 ? SynthesisBatchStatus.FAILED : SynthesisBatchStatus.PENDING;
        } else if (doneCount == acceptedCount) {
            status = failedJobCount == 0
                    ? SynthesisBatchStatus.COMPLETED
                    : SynthesisBatchStatus.COMPLETED_WITH_ERRORS;
        } else if (processingCount > 0) {
            status = SynthesisBatchStatus.PROCESSING;
        } else {
            status = SynthesisBatchStatus.PENDING;
        }

        return new SynthesisBatchStatusResponse(
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

    @Transactional(readOnly = true)
    public SynthesisJobResponse getSynthesisJob(UUID jobId) {
        SynthesisJob job = synthesisJobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "합성 작업을 찾을 수 없습니다"));
        return synthesisResponseMapper.toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public SynthesisListResponse listSynthesisJobs() {
        List<SynthesisJobResponse> items = synthesisJobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(synthesisResponseMapper::toJobResponse)
                .toList();
        return new SynthesisListResponse(items);
    }
}
