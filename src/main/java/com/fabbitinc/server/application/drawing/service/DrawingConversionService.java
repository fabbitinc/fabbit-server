package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.io.IOException;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DrawingConversionService {

    private static final String DEFAULT_PROFILE_KEY = "default";

    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final DrawingConverterProperties drawingConverterProperties;
    private final DrawingArtifactService drawingArtifactService;
    private final DrawingProcessingJobService drawingProcessingJobService;
    private final DrawingServingProjectionService drawingServingProjectionService;
    private final DrawingSourceClassifier drawingSourceClassifier;
    private final DrawingPipelineResolver drawingPipelineResolver;

    public DrawingConversionService(
            DrawingRepository drawingRepository,
            FileRepository fileRepository,
            StoragePort storagePort,
            DrawingConverterProperties drawingConverterProperties,
            DrawingArtifactService drawingArtifactService,
            DrawingProcessingJobService drawingProcessingJobService,
            DrawingServingProjectionService drawingServingProjectionService,
            DrawingSourceClassifier drawingSourceClassifier,
            DrawingPipelineResolver drawingPipelineResolver
    ) {
        this.drawingRepository = drawingRepository;
        this.fileRepository = fileRepository;
        this.storagePort = storagePort;
        this.drawingConverterProperties = drawingConverterProperties;
        this.drawingArtifactService = drawingArtifactService;
        this.drawingProcessingJobService = drawingProcessingJobService;
        this.drawingServingProjectionService = drawingServingProjectionService;
        this.drawingSourceClassifier = drawingSourceClassifier;
        this.drawingPipelineResolver = drawingPipelineResolver;
    }

    public void requestAndConvertDrawing(UUID drawingId) {
        Drawing drawing = drawingRepository.findById(drawingId).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            log.warn("event=drawing_conversion_skipped drawing_id={} reason=drawing_not_found", drawingId);
            return;
        }

        File sourceFile = resolveSourceFile(drawing);
        if (sourceFile == null) {
            log.warn("event=drawing_conversion_skipped drawing_id={} reason=source_file_not_found", drawingId);
            markConversionFailed(drawingId, "source_file_not_found");
            return;
        }

        DrawingSourceDescriptor sourceDescriptor = resolveSourceDescriptor(drawing, sourceFile);
        DrawingPipeline pipeline = drawingPipelineResolver.resolve(
                sourceDescriptor.sourceType(),
                sourceDescriptor.dimension(),
                DEFAULT_PROFILE_KEY
        );

        UUID jobId = drawingProcessingJobService.request(drawingId, pipeline.key(), DEFAULT_PROFILE_KEY);
        if (jobId == null) {
            return;
        }

        processRequestedJob(jobId);
    }

    private void processRequestedJob(UUID jobId) {
        DrawingJobClaim claim = drawingProcessingJobService.claim(jobId);
        if (claim == null) {
            return;
        }

        Drawing drawing = drawingRepository.findById(claim.drawingId()).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            drawingProcessingJobService.fail(jobId, "drawing_not_found");
            return;
        }

        File sourceFile = resolveSourceFile(drawing);
        if (sourceFile == null) {
            drawingProcessingJobService.fail(jobId, "source_file_not_found");
            return;
        }

        List<DrawingArtifactPublication> publications = List.of();
        try {
            publications = processDrawing(drawing, sourceFile, claim.profileKey());
            boolean completed = drawingProcessingJobService.complete(jobId, publications);
            if (!completed) {
                drawingArtifactService.cleanupPublishedArtifacts(publications);
            }
        } catch (Exception ex) {
            if (!publications.isEmpty()) {
                drawingArtifactService.cleanupPublishedArtifacts(publications);
            }
            drawingProcessingJobService.fail(jobId, ex.getMessage());
            log.error(
                    "event=drawing_conversion_job_failed drawing_id={} job_id={} file_key={} reason={}",
                    drawing.getId(),
                    jobId,
                    sourceFile.getFileKey(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    private List<DrawingArtifactPublication> processDrawing(
            Drawing drawing,
            File sourceFile,
            String profileKey
    ) throws Exception {
        DrawingSourceDescriptor sourceDescriptor = resolveSourceDescriptor(drawing, sourceFile);
        DrawingPipeline pipeline = drawingPipelineResolver.resolve(
                sourceDescriptor.sourceType(),
                sourceDescriptor.dimension(),
                profileKey
        );

        Path workDir = createWorkDirectory(drawing.getId());
        try {
            Path inputPath = workDir.resolve(sourceFile.getOriginalName());
            Files.createDirectories(inputPath.getParent());
            Files.write(inputPath, storagePort.getObject(sourceFile.getFileKey()));

            DrawingPipelineDeadlineContext.bind(Duration.ofSeconds(drawingConverterProperties.pipelineTimeoutSeconds()));
            try {
                DrawingPipelineResult result = pipeline.process(new DrawingPipelineCommand(
                        drawing.getId(),
                        sourceFile,
                        inputPath,
                        workDir
                ));
                return drawingArtifactService.publish(drawing.getId(), sourceFile, result.artifacts());
            } finally {
                DrawingPipelineDeadlineContext.clear();
            }
        } finally {
            cleanupWorkDirectory(workDir);
        }
    }

    private File resolveSourceFile(Drawing drawing) {
        if (drawing.isRenderSourceRequired()) {
            return null;
        }
        if (drawing.getSourceFileId() != null) {
            File sourceFile = fileRepository.findByIdAndDeletedAtIsNull(drawing.getSourceFileId()).orElse(null);
            if (sourceFile != null) {
                return sourceFile;
            }
        }
        String originalFileKey = drawing.getOriginalFileKey();
        if (originalFileKey == null || originalFileKey.isBlank()) {
            return null;
        }
        return fileRepository.findByFileKeyAndDeletedAtIsNull(originalFileKey).orElse(null);
    }

    private DrawingSourceDescriptor resolveSourceDescriptor(Drawing drawing, File sourceFile) {
        return drawingSourceClassifier.classify(sourceFile.getOriginalName());
    }

    private void markConversionFailed(UUID drawingId, String reason) {
        Drawing drawing = drawingRepository.findById(drawingId).orElse(null);
        if (drawing == null || drawing.getDeletedAt() != null) {
            return;
        }

        drawing.markConversionFailed();
        drawingRepository.save(drawing);
        if (drawingServingProjectionService != null) {
            drawingServingProjectionService.upsert(drawing);
        }
        log.warn("event=drawing_conversion_marked_failed drawing_id={} reason={}", drawingId, reason);
    }

    private Path createWorkDirectory(UUID drawingId) throws IOException {
        Path baseDir = Paths.get(drawingConverterProperties.tempDir());
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, "drawing-" + drawingId + "-");
    }

    private void cleanupWorkDirectory(Path workDir) {
        if (workDir == null) {
            return;
        }
        try (var walk = Files.walk(workDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
