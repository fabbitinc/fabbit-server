package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingJobStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingProcessingJob;
import com.fabbitinc.server.domain.drawing.repository.DrawingProcessingJobRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DrawingProcessingJobService {

    private final DrawingRepository drawingRepository;
    private final DrawingProcessingJobRepository drawingProcessingJobRepository;
    private final DrawingServingProjectionService drawingServingProjectionService;

    @Transactional
    public UUID request(UUID drawingId, String pipelineKey, String profileKey) {
        Drawing drawing = drawingRepository.findById(drawingId).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            return null;
        }

        UUID currentJobId = drawing.getCurrentJobId();
        if (currentJobId != null) {
            DrawingProcessingJob existingJob = drawingProcessingJobRepository.findById(currentJobId).orElse(null);
            if (existingJob != null && !existingJob.isTerminal()) {
                return existingJob.getId();
            }
        }

        DrawingProcessingJob job = DrawingProcessingJob.request(drawingId, pipelineKey, profileKey);
        drawingProcessingJobRepository.save(job);
        drawing.beginProcessing(job.getId());
        drawingServingProjectionService.upsert(drawing);
        return job.getId();
    }

    @Transactional
    public DrawingJobClaim claim(UUID jobId) {
        DrawingProcessingJob job = drawingProcessingJobRepository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || !job.canStart()) {
            return null;
        }
        job.start();
        return new DrawingJobClaim(job.getId(), job.getDrawingId(), job.getPipelineKey(), job.getProfileKey());
    }

    @Transactional
    public boolean complete(UUID jobId, List<DrawingArtifactPublication> artifacts) {
        DrawingProcessingJob job = drawingProcessingJobRepository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || job.getStatus() != DrawingJobStatus.PROCESSING) {
            return false;
        }

        Drawing drawing = drawingRepository.findById(job.getDrawingId()).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            job.fail("drawing_deleted");
            return false;
        }

        drawing.completeProcessing(jobId, artifacts);
        job.complete();
        drawingServingProjectionService.upsert(drawing);
        return true;
    }

    @Transactional
    public void fail(UUID jobId, String reason) {
        DrawingProcessingJob job = drawingProcessingJobRepository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || job.isTerminal()) {
            return;
        }

        Drawing drawing = drawingRepository.findById(job.getDrawingId()).orElse(null);
        if (drawing != null && drawing.getDeletedAt() == null) {
            drawing.failProcessing(jobId);
            drawingServingProjectionService.upsert(drawing);
        }
        job.fail(reason);
    }
}
