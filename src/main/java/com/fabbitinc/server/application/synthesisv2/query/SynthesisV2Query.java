package com.fabbitinc.server.application.synthesisv2.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.synthesis.support.SynthesisResponseMapper;
import com.fabbitinc.server.application.synthesisv2.query.condition.SynthesisV2BatchCondition;
import com.fabbitinc.server.application.synthesisv2.query.condition.SynthesisV2JobCondition;
import com.fabbitinc.server.application.synthesisv2.query.condition.SynthesisV2ListCondition;
import com.fabbitinc.server.application.synthesisv2.query.result.SynthesisV2BatchStatusResult;
import com.fabbitinc.server.application.synthesisv2.query.result.SynthesisV2JobResult;
import com.fabbitinc.server.application.synthesisv2.query.result.SynthesisV2ListResult;
import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Batch;
import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Job;
import com.fabbitinc.server.domain.synthesisv2.repository.SynthesisV2BatchRepository;
import com.fabbitinc.server.domain.synthesisv2.repository.SynthesisV2JobRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SynthesisV2Query {

    private final SynthesisV2BatchRepository synthesisV2BatchRepository;
    private final SynthesisV2JobRepository synthesisV2JobRepository;
    private final SynthesisResponseMapper synthesisResponseMapper;

    public SynthesisV2BatchStatusResult getBatch(SynthesisV2BatchCondition condition) {
        UUID batchId = condition.batchId();
        SynthesisV2Batch batch = synthesisV2BatchRepository.findById(batchId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 합성 배치를 찾을 수 없습니다"));

        List<SynthesisV2Job> jobs = synthesisV2JobRepository.findByBatchIdOrderByCreatedAtAsc(batchId);
        int pendingCount = 0;
        int processingCount = 0;
        int completedCount = 0;
        int failedJobCount = 0;
        List<SynthesisV2BatchStatusResult.SynthesisV2BatchItemStatusResult> items = new java.util.ArrayList<>();
        for (SynthesisV2Job job : jobs) {
            switch (job.getStatus()) {
                case PENDING -> pendingCount++;
                case PROCESSING -> processingCount++;
                case FAILED -> failedJobCount++;
                case COMPLETED -> completedCount++;
            }
            items.add(new SynthesisV2BatchStatusResult.SynthesisV2BatchItemStatusResult(
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

        List<SynthesisV2BatchStatusResult.SynthesisV2BatchFailureResult> failed = synthesisResponseMapper.parseFailures(
                        batch.getFailedUploads()
                ).stream()
                .map(item -> new SynthesisV2BatchStatusResult.SynthesisV2BatchFailureResult(
                        item.fileId(),
                        item.reason()
                ))
                .toList();
        int failedCount = failed.size();
        int acceptedCount = batch.getAcceptedCount();
        int doneCount = completedCount + failedJobCount;

        SynthesisV2BatchStatusResult.Status status;
        if (acceptedCount == 0) {
            status = failedCount > 0
                    ? SynthesisV2BatchStatusResult.Status.FAILED
                    : SynthesisV2BatchStatusResult.Status.PENDING;
        } else if (doneCount == acceptedCount) {
            status = failedJobCount == 0
                    ? SynthesisV2BatchStatusResult.Status.COMPLETED
                    : SynthesisV2BatchStatusResult.Status.COMPLETED_WITH_ERRORS;
        } else if (processingCount > 0) {
            status = SynthesisV2BatchStatusResult.Status.PROCESSING;
        } else {
            status = SynthesisV2BatchStatusResult.Status.PENDING;
        }

        return new SynthesisV2BatchStatusResult(
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

    public SynthesisV2JobResult getJob(SynthesisV2JobCondition condition) {
        UUID jobId = condition.jobId();
        SynthesisV2Job job = synthesisV2JobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 합성 작업을 찾을 수 없습니다"));
        return toSynthesisJobResult(job);
    }

    public SynthesisV2ListResult list(SynthesisV2ListCondition condition) {
        List<SynthesisV2JobResult> items = synthesisV2JobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSynthesisJobResult)
                .toList();
        return new SynthesisV2ListResult(items);
    }

    private SynthesisV2JobResult toSynthesisJobResult(SynthesisV2Job job) {
        return new SynthesisV2JobResult(
                job.getId(),
                job.getMappingId(),
                job.getFileId(),
                job.getStatus(),
                job.getTotalRows(),
                job.getProcessedRows(),
                job.getNodesCreated(),
                job.getRelationshipsCreated(),
                synthesisResponseMapper.parseErrors(job.getErrors()),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}
