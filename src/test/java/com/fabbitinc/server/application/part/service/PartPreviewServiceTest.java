package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.drawing.service.DrawingSourceClassifier;
import com.fabbitinc.server.application.drawing.service.DrawingSourceDescriptor;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingExtension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartPreviewFileRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartPreviewServiceTest {

    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private PartPreviewRepository partPreviewRepository;
    @Mock
    private PartPreviewFileRepository partPreviewFileRepository;
    @Mock
    private DrawingRepository drawingRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private OrganizationApi organizationApi;
    @Mock
    private PartPreviewArtifactService partPreviewArtifactService;
    @Mock
    private PartPreviewArtifactCleanupService partPreviewArtifactCleanupService;
    @Mock
    private PartPreviewAsyncConversionService partPreviewAsyncConversionService;
    @Mock
    private PartPreviewServingProjectionService partPreviewServingProjectionService;
    @Mock
    private DrawingSourceClassifier drawingSourceClassifier;

    @Test
    void uploadPreviewFile_기존_preview_전용_파일을_유지하고_새_파일을_현재_소스로_설정한다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());

        File existingFile = createUploadedFile("existing.step", 100L);
        File newFile = createUploadedFile("next.step", 200L);

        var existingPreviewFile = partPreview.addPreviewFile(existingFile.getId());
        existingFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, existingPreviewFile.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, existingPreviewFile.getId(), DrawingDimension.THREE_D);

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(fileRepository.findByIdAndDeletedAtIsNull(newFile.getId())).thenReturn(Optional.of(newFile));
        when(partPreviewFileRepository.findByIdAndPartPreview_Id(any(), any()))
                .thenAnswer(invocation -> partPreview.getPreviewFiles().stream()
                        .filter(file -> file.getId().equals(invocation.getArgument(0, UUID.class)))
                        .findFirst());
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingSourceClassifier.classify(newFile.getOriginalName())).thenReturn(
                new DrawingSourceDescriptor(DrawingExtension.STEP, DrawingSourceType.CAD_3D, DrawingDimension.THREE_D)
        );

        PartPreview updated = createService().uploadPreviewFile(revision.getId(), newFile.getId());

        assertEquals(2, updated.getPreviewFiles().size());
        assertEquals(PartPreviewSourceType.PREVIEW_FILE, updated.getSourceType());
        assertTrue(updated.getPreviewFiles().stream().anyMatch(file -> file.getId().equals(existingPreviewFile.getId())));
        assertTrue(updated.getPreviewFiles().stream().anyMatch(file -> file.getFileId().equals(newFile.getId())));
        assertEquals(
                newFile.getOwnerId(),
                updated.getPreviewFiles().stream()
                        .filter(file -> file.getFileId().equals(newFile.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getId()
        );
        verify(organizationApi).consumeStorageForCurrentTenant(200L);
        verify(partPreviewAsyncConversionService).convertPartPreviewAsync(updated.getId(), isNull());
    }

    private File createUploadedFile(String originalName, long fileSize) {
        File file = File.create(
                UUID.randomUUID(),
                originalName,
                "tenants/org/" + originalName,
                "application/octet-stream",
                fileSize
        );
        file.markUploaded();
        return file;
    }

    private PartPreviewService createService() {
        return new PartPreviewService(
                partRevisionRepository,
                partPreviewRepository,
                partPreviewFileRepository,
                drawingRepository,
                fileRepository,
                organizationApi,
                partPreviewArtifactService,
                partPreviewArtifactCleanupService,
                partPreviewAsyncConversionService,
                partPreviewServingProjectionService,
                drawingSourceClassifier
        );
    }
}
