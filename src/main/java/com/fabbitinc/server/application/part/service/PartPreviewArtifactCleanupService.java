package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewArtifact;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartPreviewArtifactCleanupService {

    private final FileRepository fileRepository;
    private final OrganizationApi organizationApi;

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
        fileRepository.findByFileKeyAndDeletedAtIsNull(storageKey)
                .ifPresent(file -> {
                    long fileSize = file.getFileSize();
                    file.softDelete(null);
                    if (fileSize > 0L) {
                        organizationApi.releaseStorageForCurrentTenant(fileSize);
                    }
                });
    }
}
