package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.drawing.service.DrawingPipelineArtifact;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewArtifact;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartPreviewArtifactService {

    private static final String OWNER_TYPE = "part_preview";

    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final OrganizationApi organizationApi;

    public List<DrawingArtifactPublication> publish(
            UUID partPreviewId,
            File sourceFile,
            List<DrawingPipelineArtifact> artifacts
    ) {
        List<DrawingArtifactPublication> publications = new ArrayList<>();
        try {
            for (DrawingPipelineArtifact artifact : artifacts) {
                if (artifact.reuseSource()) {
                    publications.add(new DrawingArtifactPublication(
                            artifact.artifactType(),
                            sourceFile.getId(),
                            extractFormat(sourceFile.getOriginalName()),
                            sourceFile.getFileKey(),
                            sourceFile.getContentType(),
                            sourceFile.getFileSize(),
                            false
                    ));
                    continue;
                }

                String storageKey = buildStorageKey(sourceFile.getFileKey(), artifact);
                storagePort.putObject(storageKey, artifact.bytes(), artifact.contentType());

                File generatedFile = upsertGeneratedFile(
                        partPreviewId,
                        storageKey,
                        artifact.originalName(),
                        artifact.contentType(),
                        artifact.bytes().length
                );

                publications.add(new DrawingArtifactPublication(
                        artifact.artifactType(),
                        generatedFile.getId(),
                        extractFormat(generatedFile.getOriginalName()),
                        generatedFile.getFileKey(),
                        generatedFile.getContentType(),
                        generatedFile.getFileSize(),
                        true
                ));
            }
            return publications;
        } catch (RuntimeException ex) {
            cleanupPublishedArtifacts(publications);
            throw ex;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupPublishedArtifacts(List<DrawingArtifactPublication> publications) {
        for (DrawingArtifactPublication publication : publications) {
            if (!publication.generated()) {
                continue;
            }
            deleteGeneratedFile(publication.storageKey());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupPreviewArtifacts(PartPreview partPreview) {
        for (PartPreviewArtifact artifact : partPreview.getArtifacts()) {
            if (artifact.getArtifactType() == DrawingArtifactType.SOURCE_ORIGINAL) {
                continue;
            }
            deleteGeneratedFile(artifact.getStorageKey());
        }
    }

    private void deleteGeneratedFile(String storageKey) {
        try {
            storagePort.deleteObject(storageKey);
        } catch (RuntimeException ignored) {
        }
        fileRepository.findByFileKeyAndDeletedAtIsNull(storageKey)
                .ifPresent(file -> {
                    long fileSize = file.getFileSize();
                    file.softDelete();
                    fileRepository.save(file);
                    if (fileSize > 0L) {
                        organizationApi.releaseStorageForCurrentTenant(fileSize);
                    }
                });
    }

    private File upsertGeneratedFile(
            UUID partPreviewId,
            String fileKey,
            String originalName,
            String contentType,
            long fileSize
    ) {
        File generatedFile = fileRepository.findByFileKeyAndDeletedAtIsNull(fileKey)
                .orElseGet(() -> File.create(UuidV7Generator.next(), originalName, fileKey, contentType, fileSize));
        boolean consumedStorage = false;

        generatedFile.changeStoredObject(fileKey, contentType, fileSize);
        if (generatedFile.getStatus() != FileStatus.UPLOADED) {
            generatedFile.markUploaded();
        }
        if (generatedFile.getOwnerId() == null) {
            generatedFile.assignOwner(OWNER_TYPE, partPreviewId);
            if (generatedFile.getFileSize() > 0L) {
                organizationApi.consumeStorageForCurrentTenant(generatedFile.getFileSize());
                consumedStorage = true;
            }
        }

        try {
            return fileRepository.save(generatedFile);
        } catch (RuntimeException ex) {
            if (consumedStorage) {
                organizationApi.releaseStorageForCurrentTenant(generatedFile.getFileSize());
            }
            throw ex;
        }
    }

    private String buildStorageKey(String sourceFileKey, DrawingPipelineArtifact artifact) {
        return switch (artifact.artifactType()) {
            case DERIVED_PDF -> replaceSuffix(sourceFileKey, ".pdf");
            case DERIVED_WEBP -> buildPreviewKey(sourceFileKey, artifact.originalName());
            case DERIVED_GLB -> replaceSuffix(sourceFileKey, suffixOrDefault(artifact.originalName(), ".glb"));
            case SOURCE_ORIGINAL, SOURCE_RENDER -> sourceFileKey;
        };
    }

    private String buildPreviewKey(String sourceFileKey, String originalName) {
        String suffix = suffixOrDefault(originalName, ".webp");
        String replaced = replaceSuffix(sourceFileKey, suffix);
        if (replaced.equals(sourceFileKey)) {
            return replaceSuffix(sourceFileKey, "_thumbnail" + suffix);
        }
        return replaced;
    }

    private String replaceSuffix(String value, String replacement) {
        int idx = value.lastIndexOf('.');
        if (idx < 0) {
            return value + replacement;
        }
        return value.substring(0, idx) + replacement;
    }

    private String suffixOrDefault(String value, String defaultSuffix) {
        int idx = value == null ? -1 : value.lastIndexOf('.');
        if (idx < 0 || idx >= value.length() - 1) {
            return defaultSuffix;
        }
        return value.substring(idx);
    }

    private String extractFormat(String value) {
        int idx = value == null ? -1 : value.lastIndexOf('.');
        if (idx < 0 || idx >= value.length() - 1) {
            return null;
        }
        return value.substring(idx + 1).trim().toLowerCase();
    }
}
