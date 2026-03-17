package com.fabbitinc.server.application.part.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.mapping.api.MappingApi;
import com.fabbitinc.server.application.part.query.condition.PartDraftDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewSourcesCondition;
import com.fabbitinc.server.application.part.query.result.PartDetailResult;
import com.fabbitinc.server.application.part.query.result.PartPreviewSourcesResult;
import com.fabbitinc.server.application.project.api.ProjectApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartPreviewProcessingJobRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewServingProjectionRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionHistoryRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartQueryTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;
    @Mock
    private MappingApi mappingApi;
    @Mock
    private PartRepository partRepository;
    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private EngineeringBomItemRepository engineeringBomItemRepository;
    @Mock
    private PartSupplierRepository partSupplierRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private DrawingRepository drawingRepository;
    @Mock
    private PartRevisionHistoryRepository partRevisionHistoryRepository;
    @Mock
    private PartPreviewRepository partPreviewRepository;
    @Mock
    private PartPreviewProcessingJobRepository partPreviewProcessingJobRepository;
    @Mock
    private PartPreviewServingProjectionRepository partPreviewServingProjectionRepository;
    @Mock
    private ProjectApi projectApi;
    @Mock
    private UserApi userApi;
    @Mock
    private FileUrlResolver fileUrlResolver;
    @Mock
    private EntityManager entityManager;

    private PartQuery partQuery;

    @BeforeEach
    void setUp() {
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(UUID.randomUUID(), "a@b.c", UUID.randomUUID(), null));
        when(fileUrlResolver.resolve(anyString())).thenAnswer(invocation -> "url/" + invocation.getArgument(0, String.class));

        partQuery = new PartQuery(
                currentAuthProvider,
                mappingApi,
                partRepository,
                partRevisionRepository,
                fileRepository,
                engineeringBomItemRepository,
                partSupplierRepository,
                supplierRepository,
                drawingRepository,
                partRevisionHistoryRepository,
                partPreviewRepository,
                partPreviewProcessingJobRepository,
                partPreviewServingProjectionRepository,
                projectApi,
                userApi,
                fileUrlResolver,
                entityManager
        );
    }

    @Test
    void getPreviewSources_preview_전용_파일이_두개면_둘다_조회된다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-100");
        PartRevision draft = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        PartPreview partPreview = PartPreview.create(draft.getId());

        File firstFile = createUploadedFile("first.step", 100L);
        File secondFile = createUploadedFile("second.step", 200L);
        var firstPreviewFile = partPreview.addPreviewFile(firstFile.getId());
        var secondPreviewFile = partPreview.addPreviewFile(secondFile.getId());
        firstFile.assignOwner("part_preview_file", firstPreviewFile.getId());
        secondFile.assignOwner("part_preview_file", secondPreviewFile.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, secondPreviewFile.getId(), DrawingDimension.THREE_D);

        when(partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionIdIsNull("P-100", "D1"))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(draft.getId()))
                .thenReturn(List.of());
        when(partPreviewRepository.findByPartRevisionId(draft.getId())).thenReturn(Optional.of(partPreview));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                firstFile.getId(), "part_preview_file", firstPreviewFile.getId()
        )).thenReturn(Optional.of(firstFile));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                secondFile.getId(), "part_preview_file", secondPreviewFile.getId()
        )).thenReturn(Optional.of(secondFile));

        PartPreviewSourcesResult result = partQuery.getPreviewSources(
                new PartPreviewSourcesCondition("P-100", null, null, "D1")
        );

        assertEquals(2, result.total());
        assertEquals(2, result.items().size());
        assertTrue(result.items().stream().anyMatch(item -> item.sourceId().equals(firstPreviewFile.getId())));
        assertTrue(result.items().stream().anyMatch(item -> item.sourceId().equals(secondPreviewFile.getId()) && item.selected()));
    }

    @Test
    void getDraft_원본_파일이_없는_도면은_files_count에_포함하지_않는다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-200");
        PartRevision draft = PartRevision.createInitialDraft(part, "D1", "part", actorId);
        UUID missingSourceFileId = UUID.randomUUID();

        Drawing danglingDrawing = Drawing.create("DRW-1", "도면");
        danglingDrawing.assignPartRevision(draft.getId());
        danglingDrawing.registerSourceFile(
                missingSourceFileId,
                DrawingDimension.TWO_D,
                "tenants/org/missing.pdf",
                "application/pdf",
                10L
        );

        when(partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionIdIsNull("P-200", "D1"))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId())).thenReturn(List.of(draft));
        when(partPreviewRepository.findByPartRevisionId(draft.getId())).thenReturn(Optional.empty());
        when(partSupplierRepository.countByPartRevisionId(draft.getId())).thenReturn(0L);
        when(fileRepository.countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                "part_revision",
                draft.getId(),
                FileStatus.UPLOADED
        )).thenReturn(0L);
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(draft.getId()))
                .thenReturn(List.of(danglingDrawing));
        when(fileRepository.findByIdAndDeletedAtIsNull(missingSourceFileId)).thenReturn(Optional.empty());
        when(fileRepository.findByFileKeyAndDeletedAtIsNull("tenants/org/missing.pdf")).thenReturn(Optional.empty());
        when(engineeringBomItemRepository.countByParentPartRevisionId(draft.getId())).thenReturn(0L);
        when(engineeringBomItemRepository.countByChildPartRevisionId(draft.getId())).thenReturn(0L);
        when(projectApi.countPartProjects(part.getId())).thenReturn(0L);

        PartDetailResult result = partQuery.getDraft(new PartDraftDetailCondition("P-200", null, "D1"));

        assertEquals(0L, result.filesCount());
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
}
