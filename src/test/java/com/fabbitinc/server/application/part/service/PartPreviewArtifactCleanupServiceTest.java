package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartPreviewArtifactCleanupServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private OrganizationApi organizationApi;

    @Test
    void cleanupArtifactFiles_생성_파일만_soft_delete한다() {
        File generatedFile = createUploadedFile("preview.webp", "tenants/org/preview.webp", 120L);
        generatedFile.assignOwner(PartPreviewArtifactService.OWNER_TYPE, UUID.randomUUID());
        when(fileRepository.findByFileKeyAndOwnerTypeAndDeletedAtIsNull(
                generatedFile.getFileKey(),
                PartPreviewArtifactService.OWNER_TYPE
        )).thenReturn(Optional.of(generatedFile));

        PartPreviewArtifactCleanupService service = createService();

        service.cleanupArtifactFiles(List.of(generatedFile.getFileKey()));

        assertNotNull(generatedFile.getDeletedAt());
        verify(organizationApi).releaseStorageForCurrentTenant(120L);
    }

    @Test
    void cleanupArtifactFiles_preview_원본_파일은_삭제하지_않는다() {
        File sourceFile = createUploadedFile("preview.pdf", "tenants/org/preview.pdf", 240L);
        sourceFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, UUID.randomUUID());
        when(fileRepository.findByFileKeyAndOwnerTypeAndDeletedAtIsNull(
                sourceFile.getFileKey(),
                PartPreviewArtifactService.OWNER_TYPE
        )).thenReturn(Optional.empty());

        PartPreviewArtifactCleanupService service = createService();

        service.cleanupArtifactFiles(List.of(sourceFile.getFileKey()));

        assertNull(sourceFile.getDeletedAt());
        verify(organizationApi, never()).releaseStorageForCurrentTenant(240L);
    }

    private File createUploadedFile(String originalName, String fileKey, long fileSize) {
        File file = File.create(UUID.randomUUID(), originalName, fileKey, "application/octet-stream", fileSize);
        file.markUploaded();
        return file;
    }

    private PartPreviewArtifactCleanupService createService() {
        return new PartPreviewArtifactCleanupService(fileRepository, organizationApi);
    }
}
