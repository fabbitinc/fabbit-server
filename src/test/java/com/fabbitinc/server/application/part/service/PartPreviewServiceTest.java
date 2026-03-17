package com.fabbitinc.server.application.part.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.drawing.service.DrawingSourceClassifier;
import com.fabbitinc.server.application.drawing.service.DrawingSourceDescriptor;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.drawing.model.Drawing;
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

    @Test
    void uploadPreviewFile_이미_다른_리소스에_연결된_파일이면_거부된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-108");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        File file = createUploadedFile("owned.step", 100L);
        file.assignOwner("part_revision", UUID.randomUUID());

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.empty());
        when(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).thenReturn(Optional.of(file));

        AppException ex = assertThrows(AppException.class, () -> createService().uploadPreviewFile(revision.getId(), file.getId()));

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(partPreviewAsyncConversionService, never()).convertPartPreviewAsync(any(), any());
    }

    @Test
    void uploadPreviewFile_기존_preview가_없는_초기_draft에서도_바로_등록할_수_있다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-102");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        File newFile = createUploadedFile("first.step", 300L);

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.empty());
        when(fileRepository.findByIdAndDeletedAtIsNull(newFile.getId())).thenReturn(Optional.of(newFile));
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingSourceClassifier.classify(newFile.getOriginalName())).thenReturn(
                new DrawingSourceDescriptor(DrawingExtension.STEP, DrawingSourceType.CAD_3D, DrawingDimension.THREE_D)
        );

        PartPreview created = createService().uploadPreviewFile(revision.getId(), newFile.getId());

        assertEquals(PartPreviewSourceType.PREVIEW_FILE, created.getSourceType());
        assertEquals(1, created.getPreviewFiles().size());
        assertEquals(newFile.getId(), created.getPreviewFiles().getFirst().getFileId());
        assertEquals(created.getPreviewFiles().getFirst().getId(), newFile.getOwnerId());
        verify(partPreviewRepository).save(any(PartPreview.class));
        verify(partPreviewAsyncConversionService).convertPartPreviewAsync(created.getId(), isNull());
    }

    @Test
    void changeSource_도면에서_preview파일로_전환해도_preview파일목록은_유지된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-103");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());

        Drawing drawing = Drawing.create("D-1", "도면");
        drawing.assignPartRevision(revision.getId());
        File drawingFile = createUploadedFile("source.pdf", 120L);
        drawing.assignSourceFile(drawingFile.getId(), DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D);
        drawing.changeOriginalFileKey(drawingFile.getFileKey());

        File previewFile = createUploadedFile("next.step", 220L);
        var previewFileRelation = partPreview.addPreviewFile(previewFile.getId());
        previewFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, previewFileRelation.getId());
        partPreview.replaceSource(PartPreviewSourceType.DRAWING, drawing.getId(), DrawingDimension.TWO_D);

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(drawingRepository.findById(drawing.getId())).thenReturn(Optional.of(drawing));
        when(fileRepository.findByIdAndDeletedAtIsNull(drawingFile.getId())).thenReturn(Optional.of(drawingFile));
        when(partPreviewFileRepository.findByIdAndPartPreview_Id(previewFileRelation.getId(), partPreview.getId()))
                .thenReturn(Optional.of(previewFileRelation));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                previewFile.getId(),
                PartPreviewService.OWNER_TYPE_PREVIEW_FILE,
                previewFileRelation.getId()
        )).thenReturn(Optional.of(previewFile));
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingSourceClassifier.classify(drawingFile.getOriginalName())).thenReturn(
                new DrawingSourceDescriptor(DrawingExtension.PDF, DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D)
        );
        when(drawingSourceClassifier.classify(previewFile.getOriginalName())).thenReturn(
                new DrawingSourceDescriptor(DrawingExtension.STEP, DrawingSourceType.CAD_3D, DrawingDimension.THREE_D)
        );

        PartPreview updated = createService().changeSource(revision.getId(), PartPreviewSourceType.PREVIEW_FILE, previewFileRelation.getId());

        assertEquals(1, updated.getPreviewFiles().size());
        assertEquals(previewFileRelation.getId(), updated.getPreviewFiles().getFirst().getId());
        assertEquals(PartPreviewSourceType.PREVIEW_FILE, updated.getSourceType());
        assertEquals(previewFileRelation.getId(), updated.getSourceId());
        verify(partPreviewAsyncConversionService).convertPartPreviewAsync(updated.getId(), isNull());
    }

    @Test
    void changeSource_preview파일에서_도면으로_전환해도_preview파일목록은_유지된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-106");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());

        File previewFile = createUploadedFile("selected.step", 220L);
        var previewFileRelation = partPreview.addPreviewFile(previewFile.getId());
        previewFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, previewFileRelation.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, previewFileRelation.getId(), DrawingDimension.THREE_D);

        Drawing drawing = Drawing.create("D-2", "도면");
        drawing.assignPartRevision(revision.getId());
        File drawingFile = createUploadedFile("source.pdf", 120L);
        drawing.assignSourceFile(drawingFile.getId(), DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D);
        drawing.changeOriginalFileKey(drawingFile.getFileKey());

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(drawingRepository.findById(drawing.getId())).thenReturn(Optional.of(drawing));
        when(fileRepository.findByIdAndDeletedAtIsNull(drawingFile.getId())).thenReturn(Optional.of(drawingFile));
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawingSourceClassifier.classify(drawingFile.getOriginalName())).thenReturn(
                new DrawingSourceDescriptor(DrawingExtension.PDF, DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D)
        );

        PartPreview updated = createService().changeSource(revision.getId(), PartPreviewSourceType.DRAWING, drawing.getId());

        assertEquals(1, updated.getPreviewFiles().size());
        assertEquals(previewFileRelation.getId(), updated.getPreviewFiles().getFirst().getId());
        assertEquals(PartPreviewSourceType.DRAWING, updated.getSourceType());
        assertEquals(drawing.getId(), updated.getSourceId());
        verify(partPreviewAsyncConversionService).convertPartPreviewAsync(updated.getId(), isNull());
    }

    @Test
    void uploadPreviewFile_DWG는_preview_직접변환_대상이_아니라_거부된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-101");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());
        File dwgFile = createUploadedFile("blocked.dwg", 150L);

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(fileRepository.findByIdAndDeletedAtIsNull(dwgFile.getId())).thenReturn(Optional.of(dwgFile));
        when(drawingSourceClassifier.classify(dwgFile.getOriginalName())).thenReturn(
                new DrawingSourceDescriptor(DrawingExtension.DWG, DrawingSourceType.CAD_2D, DrawingDimension.TWO_D)
        );

        AppException ex = assertThrows(
                AppException.class,
                () -> createService().uploadPreviewFile(revision.getId(), dwgFile.getId())
        );

        assertEquals(ErrorCode.PRECONDITION_FAILED, ex.getErrorCode());
        assertEquals("대표 미리보기는 직접 변환 가능한 파일만 선택할 수 있습니다", ex.getMessage());
    }

    @Test
    void changeSource_도면원본파일이_없으면_거부된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-109");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());
        Drawing drawing = Drawing.create("D-9", "도면");
        drawing.assignPartRevision(revision.getId());

        when(partRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(drawingRepository.findById(drawing.getId())).thenReturn(Optional.of(drawing));

        AppException ex = assertThrows(
                AppException.class,
                () -> createService().changeSource(revision.getId(), PartPreviewSourceType.DRAWING, drawing.getId())
        );

        assertEquals(ErrorCode.INVALID_STATE, ex.getErrorCode());
        assertEquals("도면 원본 파일이 없습니다", ex.getMessage());
    }

    @Test
    void deletePreviewFile_현재선택된_preview파일을_삭제하면_preview가_해제된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-104");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());
        File previewFile = createUploadedFile("selected.step", 210L);
        var previewFileRelation = partPreview.addPreviewFile(previewFile.getId());
        previewFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, previewFileRelation.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, previewFileRelation.getId(), DrawingDimension.THREE_D);

        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(partPreviewFileRepository.findByIdAndPartPreview_Id(previewFileRelation.getId(), partPreview.getId()))
                .thenReturn(Optional.of(previewFileRelation));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                previewFile.getId(),
                PartPreviewService.OWNER_TYPE_PREVIEW_FILE,
                previewFileRelation.getId()
        )).thenReturn(Optional.of(previewFile));
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createService().deletePreviewFile(revision.getId(), previewFileRelation.getId(), actorId);

        assertNull(partPreview.getSourceType());
        assertNull(partPreview.getSourceId());
        assertEquals(0, partPreview.getPreviewFiles().size());
        verify(organizationApi).releaseStorageForCurrentTenant(210L);
    }

    @Test
    void deletePreviewFile_선택되지_않은_preview파일을_삭제하면_현재_preview는_유지된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-105");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());
        File selectedFile = createUploadedFile("selected.step", 180L);
        File otherFile = createUploadedFile("other.step", 190L);
        var selectedRelation = partPreview.addPreviewFile(selectedFile.getId());
        var otherRelation = partPreview.addPreviewFile(otherFile.getId());
        selectedFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, selectedRelation.getId());
        otherFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, otherRelation.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, selectedRelation.getId(), DrawingDimension.THREE_D);

        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(partPreviewFileRepository.findByIdAndPartPreview_Id(otherRelation.getId(), partPreview.getId()))
                .thenReturn(Optional.of(otherRelation));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                otherFile.getId(),
                PartPreviewService.OWNER_TYPE_PREVIEW_FILE,
                otherRelation.getId()
        )).thenReturn(Optional.of(otherFile));

        createService().deletePreviewFile(revision.getId(), otherRelation.getId(), actorId);

        assertEquals(PartPreviewSourceType.PREVIEW_FILE, partPreview.getSourceType());
        assertEquals(selectedRelation.getId(), partPreview.getSourceId());
        assertEquals(1, partPreview.getPreviewFiles().size());
        assertEquals(selectedRelation.getId(), partPreview.getPreviewFiles().getFirst().getId());
        verify(organizationApi).releaseStorageForCurrentTenant(190L);
    }

    @Test
    void clearByPartRevision_현재_preview만_해제하고_preview파일목록은_유지한다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-107");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(revision.getId());
        File previewFile = createUploadedFile("selected.step", 210L);
        var previewFileRelation = partPreview.addPreviewFile(previewFile.getId());
        previewFile.assignOwner(PartPreviewService.OWNER_TYPE_PREVIEW_FILE, previewFileRelation.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, previewFileRelation.getId(), DrawingDimension.THREE_D);

        when(partPreviewRepository.findByPartRevisionId(revision.getId())).thenReturn(Optional.of(partPreview));
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createService().clearByPartRevision(revision.getId());

        assertNull(partPreview.getSourceType());
        assertNull(partPreview.getSourceId());
        assertEquals(1, partPreview.getPreviewFiles().size());
        assertEquals(previewFileRelation.getId(), partPreview.getPreviewFiles().getFirst().getId());
    }

    @Test
    void clearByPartRevision_preview가_없으면_아무일도_하지_않는다() {
        UUID partRevisionId = UUID.randomUUID();

        when(partPreviewRepository.findByPartRevisionId(partRevisionId)).thenReturn(Optional.empty());

        createService().clearByPartRevision(partRevisionId);

        verify(partPreviewRepository, never()).save(any());
        verify(partPreviewServingProjectionService, never()).upsert(any());
    }

    @Test
    void clearByDrawing_선택된_모든_preview를_해제한다() {
        PartPreview first = PartPreview.create(UUID.randomUUID());
        PartPreview second = PartPreview.create(UUID.randomUUID());
        UUID drawingId = UUID.randomUUID();

        first.replaceSource(PartPreviewSourceType.DRAWING, drawingId, DrawingDimension.TWO_D);
        second.replaceSource(PartPreviewSourceType.DRAWING, drawingId, DrawingDimension.TWO_D);

        when(partPreviewRepository.findBySourceTypeAndSourceId(PartPreviewSourceType.DRAWING, drawingId))
                .thenReturn(List.of(first, second));
        when(partPreviewRepository.save(any(PartPreview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        createService().clearByDrawing(drawingId);

        assertNull(first.getSourceType());
        assertNull(second.getSourceType());
        verify(partPreviewRepository).save(first);
        verify(partPreviewRepository).save(second);
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
