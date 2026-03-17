package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.drawing.config.DrawingConverterProperties;
import com.fabbitinc.server.application.drawing.service.DrawingPipeline;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineCommand;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineResolver;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineResult;
import com.fabbitinc.server.application.drawing.service.DrawingSourceClassifier;
import com.fabbitinc.server.application.drawing.service.DrawingSourceDescriptor;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PartPreviewConversionService {

    private static final String DEFAULT_PROFILE_KEY = "default";

    private final PartPreviewRepository partPreviewRepository;
    private final DrawingRepository drawingRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final DrawingConverterProperties drawingConverterProperties;
    private final PartPreviewArtifactService partPreviewArtifactService;
    private final PartPreviewArtifactCleanupService partPreviewArtifactCleanupService;
    private final PartPreviewProcessingJobService partPreviewProcessingJobService;
    private final PartPreviewServingProjectionService partPreviewServingProjectionService;
    private final DrawingSourceClassifier drawingSourceClassifier;
    private final DrawingPipelineResolver drawingPipelineResolver;

    public PartPreviewConversionService(
            PartPreviewRepository partPreviewRepository,
            DrawingRepository drawingRepository,
            FileRepository fileRepository,
            StoragePort storagePort,
            DrawingConverterProperties drawingConverterProperties,
            PartPreviewArtifactService partPreviewArtifactService,
            PartPreviewArtifactCleanupService partPreviewArtifactCleanupService,
            PartPreviewProcessingJobService partPreviewProcessingJobService,
            PartPreviewServingProjectionService partPreviewServingProjectionService,
            DrawingSourceClassifier drawingSourceClassifier,
            DrawingPipelineResolver drawingPipelineResolver
    ) {
        this.partPreviewRepository = partPreviewRepository;
        this.drawingRepository = drawingRepository;
        this.fileRepository = fileRepository;
        this.storagePort = storagePort;
        this.drawingConverterProperties = drawingConverterProperties;
        this.partPreviewArtifactService = partPreviewArtifactService;
        this.partPreviewArtifactCleanupService = partPreviewArtifactCleanupService;
        this.partPreviewProcessingJobService = partPreviewProcessingJobService;
        this.partPreviewServingProjectionService = partPreviewServingProjectionService;
        this.drawingSourceClassifier = drawingSourceClassifier;
        this.drawingPipelineResolver = drawingPipelineResolver;
    }

    public void requestAndConvertPartPreview(UUID partPreviewId) {
        PartPreview partPreview = partPreviewRepository.findById(partPreviewId).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            log.warn("event=part_preview_conversion_skipped part_preview_id={} reason=part_preview_not_found", partPreviewId);
            return;
        }

        File sourceFile = resolveSourceFile(partPreview);
        if (sourceFile == null) {
            log.warn("event=part_preview_conversion_skipped part_preview_id={} reason=source_file_not_found", partPreviewId);
            markConversionFailed(partPreviewId, "source_file_not_found");
            return;
        }

        DrawingSourceDescriptor sourceDescriptor = resolveSourceDescriptor(sourceFile);
        DrawingPipeline pipeline = drawingPipelineResolver.resolve(
                sourceDescriptor.sourceType(),
                sourceDescriptor.dimension(),
                DEFAULT_PROFILE_KEY
        );

        UUID jobId = partPreviewProcessingJobService.request(partPreviewId, pipeline.key(), DEFAULT_PROFILE_KEY);
        if (jobId == null) {
            return;
        }

        processRequestedJob(jobId);
    }

    private void processRequestedJob(UUID jobId) {
        PartPreviewJobClaim claim = partPreviewProcessingJobService.claim(jobId);
        if (claim == null) {
            return;
        }

        PartPreview partPreview = partPreviewRepository.findById(claim.partPreviewId()).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            partPreviewProcessingJobService.fail(jobId, "part_preview_not_found");
            return;
        }

        File sourceFile = resolveSourceFile(partPreview);
        if (sourceFile == null) {
            partPreviewProcessingJobService.fail(jobId, "source_file_not_found");
            return;
        }

        List<DrawingArtifactPublication> publications = List.of();
        try {
            publications = processPreview(partPreview, sourceFile, claim.profileKey());
            boolean completed = partPreviewProcessingJobService.complete(jobId, publications);
            if (!completed) {
                partPreviewArtifactCleanupService.cleanupPublishedArtifacts(publications);
            }
        } catch (Exception ex) {
            if (!publications.isEmpty()) {
                partPreviewArtifactCleanupService.cleanupPublishedArtifacts(publications);
            }
            partPreviewProcessingJobService.fail(jobId, ex.getMessage());
            log.error(
                    "event=part_preview_conversion_job_failed part_preview_id={} job_id={} file_key={} reason={}",
                    partPreview.getId(),
                    jobId,
                    sourceFile.getFileKey(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    private List<DrawingArtifactPublication> processPreview(
            PartPreview partPreview,
            File sourceFile,
            String profileKey
    ) throws Exception {
        DrawingSourceDescriptor sourceDescriptor = resolveSourceDescriptor(sourceFile);
        DrawingPipeline pipeline = drawingPipelineResolver.resolve(
                sourceDescriptor.sourceType(),
                sourceDescriptor.dimension(),
                profileKey
        );

        Path workDir = createWorkDirectory(partPreview.getId());
        try {
            Path inputPath = workDir.resolve(sourceFile.getOriginalName());
            Files.createDirectories(inputPath.getParent());
            Files.write(inputPath, storagePort.getObject(sourceFile.getFileKey()));

            com.fabbitinc.server.application.drawing.service.DrawingPipelineDeadlineContext.bind(
                    Duration.ofSeconds(drawingConverterProperties.pipelineTimeoutSeconds())
            );
            try {
                DrawingPipelineResult result = pipeline.process(new DrawingPipelineCommand(
                        partPreview.getId(),
                        sourceFile,
                        inputPath,
                        workDir
                ));
                return partPreviewArtifactService.publish(partPreview.getId(), sourceFile, result.artifacts());
            } finally {
                com.fabbitinc.server.application.drawing.service.DrawingPipelineDeadlineContext.clear();
            }
        } finally {
            cleanupWorkDirectory(workDir);
        }
    }

    private File resolveSourceFile(PartPreview partPreview) {
        if (partPreview.getSourceType() == PartPreviewSourceType.PREVIEW_FILE) {
            return fileRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                            PartPreviewService.OWNER_TYPE_PREVIEW_FILE,
                            partPreview.getSourceId()
                    ).stream()
                    .findFirst()
                    .orElse(null);
        }
        if (partPreview.getSourceType() != PartPreviewSourceType.DRAWING) {
            return null;
        }

        Drawing drawing = drawingRepository.findById(partPreview.getSourceId())
                .filter(it -> it.getDeletedAt() == null)
                .orElse(null);
        if (drawing == null || drawing.getSourceFileId() == null) {
            return null;
        }
        return fileRepository.findByIdAndDeletedAtIsNull(drawing.getSourceFileId()).orElse(null);
    }

    private DrawingSourceDescriptor resolveSourceDescriptor(File sourceFile) {
        DrawingSourceDescriptor descriptor = drawingSourceClassifier.classify(sourceFile.getOriginalName());
        if (!descriptor.extension().canStartPipelineDirectly()) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "대표 미리보기는 직접 변환 가능한 파일만 선택할 수 있습니다"
            );
        }
        return descriptor;
    }

    private void markConversionFailed(UUID partPreviewId, String reason) {
        PartPreview partPreview = partPreviewRepository.findById(partPreviewId).orElse(null);
        if (partPreview == null || !partPreview.hasSource()) {
            return;
        }

        partPreview.failProcessing(null);
        partPreviewServingProjectionService.upsert(partPreview);
        log.warn("event=part_preview_conversion_marked_failed part_preview_id={} reason={}", partPreviewId, reason);
    }

    private Path createWorkDirectory(UUID partPreviewId) throws IOException {
        Path baseDir = Paths.get(drawingConverterProperties.tempDir());
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, "part-preview-" + partPreviewId + "-");
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
