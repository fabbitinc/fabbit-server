package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingJob;
import com.fabbitinc.server.domain.part.repository.PartPreviewProcessingJobRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartPreviewProcessingJobService {

    private final PartPreviewRepository partPreviewRepository;
    private final PartPreviewProcessingJobRepository partPreviewProcessingJobRepository;
    private final PartPreviewServingProjectionService partPreviewServingProjectionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID request(UUID partPreviewId, String pipelineKey, String profileKey) {
        PartPreview partPreview = partPreviewRepository.findById(partPreviewId).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            return null;
        }

        UUID currentJobId = partPreview.getCurrentJobId();
        if (currentJobId != null) {
            PartPreviewProcessingJob existingJob = partPreviewProcessingJobRepository.findById(currentJobId).orElse(null);
            if (existingJob != null && !existingJob.isTerminal()) {
                return existingJob.getId();
            }
        }

        PartPreviewProcessingJob job = PartPreviewProcessingJob.request(partPreviewId, pipelineKey, profileKey);
        partPreviewProcessingJobRepository.save(job);
        partPreview.beginProcessing(job.getId());
        partPreviewServingProjectionService.upsert(partPreview);
        return job.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PartPreviewJobClaim claim(UUID jobId) {
        PartPreviewProcessingJob job = partPreviewProcessingJobRepository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || !job.canStart()) {
            return null;
        }
        job.start();
        return new PartPreviewJobClaim(job.getId(), job.getPartPreviewId(), job.getPipelineKey(), job.getProfileKey());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean complete(UUID jobId, List<DrawingArtifactPublication> artifacts) {
        PartPreviewProcessingJob job = partPreviewProcessingJobRepository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || job.getStatus() != DrawingJobStatus.PROCESSING) {
            return false;
        }

        PartPreview partPreview = partPreviewRepository.findById(job.getPartPreviewId()).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            job.fail("part_preview_source_missing");
            log.warn(
                    "event=part_preview_job_complete_failed part_preview_id={} job_id={} reason=source_missing",
                    job.getPartPreviewId(),
                    jobId
            );
            return false;
        }

        partPreview.completeProcessing(jobId, artifacts);
        job.complete();
        partPreviewServingProjectionService.upsert(partPreview);
        log.info(
                "event=part_preview_job_completed part_preview_id={} job_id={} job_status={} conversion_status_after={}",
                partPreview.getId(),
                jobId,
                job.getStatus(),
                partPreview.getConversionStatus()
        );
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID jobId, String reason) {
        PartPreviewProcessingJob job = partPreviewProcessingJobRepository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || job.isTerminal()) {
            return;
        }

        PartPreview partPreview = partPreviewRepository.findById(job.getPartPreviewId()).orElse(null);
        if (partPreview != null && partPreview.hasSource()) {
            partPreview.failProcessing(jobId);
            partPreviewServingProjectionService.upsert(partPreview);
            log.warn(
                    "event=part_preview_job_failed part_preview_id={} job_id={} reason={} conversion_status_after={} current_job_id_after={}",
                    partPreview.getId(),
                    jobId,
                    reason,
                    partPreview.getConversionStatus(),
                    partPreview.getCurrentJobId()
            );
        } else if (partPreview != null && partPreview.getConversionStatus() == DrawingConversionStatus.PENDING) {
            partPreview.failProcessing(null);
            partPreviewServingProjectionService.upsert(partPreview);
        }
        job.fail(reason);
    }
}
