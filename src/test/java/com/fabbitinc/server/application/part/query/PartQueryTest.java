package com.fabbitinc.server.application.part.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeApi;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeSnapshot;
import com.fabbitinc.server.application.mapping.api.MappingApi;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewSourcesCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionHistoryCondition;
import com.fabbitinc.server.application.part.query.result.PartBomResult;
import com.fabbitinc.server.application.part.query.result.PartDetailResult;
import com.fabbitinc.server.application.part.query.result.PartListResult;
import com.fabbitinc.server.application.part.query.result.PartLookupResult;
import com.fabbitinc.server.application.part.query.result.PartPreviewSourcesResult;
import com.fabbitinc.server.application.part.query.result.PartRevisionCreationSourceType;
import com.fabbitinc.server.application.part.query.result.PartRevisionHistoryResult;
import com.fabbitinc.server.application.part.query.result.PartRevisionReleaseWorkflowType;
import com.fabbitinc.server.application.project.api.ProjectApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionHistorySourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartCategoryRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewFileRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewProcessingJobRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewServingProjectionRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionHistoryRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartQueryTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;
    @Mock
    private EngineeringChangeApi engineeringChangeApi;
    @Mock
    private MappingApi mappingApi;
    @Mock
    private PartRepository partRepository;
    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private PartCategoryRepository partCategoryRepository;
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
    private PartPreviewFileRepository partPreviewFileRepository;
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
                engineeringChangeApi,
                mappingApi,
                partRepository,
                partRevisionRepository,
                partCategoryRepository,
                fileRepository,
                engineeringBomItemRepository,
                partSupplierRepository,
                supplierRepository,
                drawingRepository,
                partRevisionHistoryRepository,
                partPreviewRepository,
                partPreviewFileRepository,
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
        PartRevision draft = PartRevision.createInitialDraft(part, "part", actorId);
        PartPreview partPreview = PartPreview.create(draft.getId());

        File firstFile = createUploadedFile("first.step", 100L);
        File secondFile = createUploadedFile("second.step", 200L);
        var firstPreviewFile = partPreview.addPreviewFile(firstFile.getId());
        var secondPreviewFile = partPreview.addPreviewFile(secondFile.getId());
        setCreatedAt(firstPreviewFile, Instant.parse("2026-03-18T00:00:00Z"));
        setCreatedAt(secondPreviewFile, Instant.parse("2026-03-18T01:00:00Z"));
        firstFile.assignOwner("part_preview_file", firstPreviewFile.getId());
        secondFile.assignOwner("part_preview_file", secondPreviewFile.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, secondPreviewFile.getId(), DrawingDimension.THREE_D);

        when(partRevisionRepository.findByIdAndPartId(draft.getId(), part.getId()))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(draft.getId()))
                .thenReturn(List.of());
        when(partPreviewRepository.findByPartRevisionId(draft.getId())).thenReturn(Optional.of(partPreview));
        when(partPreviewFileRepository.findByPartPreview_IdOrderByCreatedAtDesc(partPreview.getId()))
                .thenReturn(List.of(secondPreviewFile, firstPreviewFile));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                firstFile.getId(), "part_preview_file", firstPreviewFile.getId()
        )).thenReturn(Optional.of(firstFile));
        when(fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                secondFile.getId(), "part_preview_file", secondPreviewFile.getId()
        )).thenReturn(Optional.of(secondFile));

        PartPreviewSourcesResult result = partQuery.getPreviewSources(
                new PartPreviewSourcesCondition(part.getId(), draft.getId())
        );

        assertEquals(2, result.total());
        assertEquals(2, result.items().size());
        assertTrue(result.items().stream().anyMatch(item -> item.sourceId().equals(firstPreviewFile.getId())));
        assertTrue(result.items().stream().anyMatch(item -> item.sourceId().equals(secondPreviewFile.getId()) && item.selected()));
    }

    @Test
    void getHistory_공식리비전과_초안_생성_폐기가_모두_보인다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-110");
        PartRevision released = PartRevision.createInitialDraft(part, "released", actorId);
        setCreatedAt(released, Instant.parse("2026-03-18T00:00:00Z"));
        released.release("1", actorId);
        released.recordHistoryAt(
                actorId,
                PartRevisionHistoryActionType.RELEASED,
                PartRevisionHistorySourceType.USER,
                null,
                "{\"reason\":\"최초 반영\"}",
                Instant.parse("2026-03-18T00:00:00Z")
        );

        PartRevision canceledDraft = PartRevision.createDraft(part, released.getId(), "draft", actorId);
        setCreatedAt(canceledDraft, Instant.parse("2026-03-18T01:00:00Z"));
        canceledDraft.recordHistoryAt(
                actorId,
                PartRevisionHistoryActionType.CREATED,
                PartRevisionHistorySourceType.USER,
                null,
                "{\"reason\":\"초안 생성\"}",
                Instant.parse("2026-03-18T01:00:00Z")
        );
        canceledDraft.cancel(actorId);
        canceledDraft.recordHistoryAt(
                actorId,
                PartRevisionHistoryActionType.CANCELED,
                PartRevisionHistorySourceType.USER,
                null,
                "{\"action\":\"CANCELED\",\"reason\":\"폐기\"}",
                Instant.parse("2026-03-18T02:00:00Z")
        );

        PartRevision releasedDraft = PartRevision.createDraft(part, released.getId(), "released-draft", actorId);
        setCreatedAt(releasedDraft, Instant.parse("2026-03-18T03:00:00Z"));
        releasedDraft.recordHistoryAt(
                actorId,
                PartRevisionHistoryActionType.CREATED,
                PartRevisionHistorySourceType.USER,
                null,
                "{\"reason\":\"초안 생성\"}",
                Instant.parse("2026-03-18T03:00:00Z")
        );
        releasedDraft.release("2", actorId);
        UUID engineeringChangeId = UUID.randomUUID();
        releasedDraft.recordHistoryAt(
                actorId,
                PartRevisionHistoryActionType.RELEASED,
                PartRevisionHistorySourceType.ENGINEERING_CHANGE,
                engineeringChangeId,
                "{\"reason\":\"\"}",
                Instant.parse("2026-03-18T04:00:00Z")
        );

        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId())).thenReturn(List.of(releasedDraft, canceledDraft, released));
        when(fileRepository.findByOwnerTypeAndOwnerIdInAndStatusAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.eq("part_revision"),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq(FileStatus.UPLOADED)
        )).thenReturn(List.of());
        when(drawingRepository.findByPartRevisionIdInAndDeletedAtIsNullOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of());
        when(engineeringBomItemRepository.findByParentPartRevisionIdInOrderByParentPartRevisionIdAscCreatedAtAsc(
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(List.of());
        when(partRevisionHistoryRepository.findByPartRevisionIdInOrderByOccurredAtAsc(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(List.of(
                        released.getHistories().getFirst(),
                        canceledDraft.getHistories().getFirst(),
                        canceledDraft.getHistories().getLast(),
                        releasedDraft.getHistories().getFirst(),
                        releasedDraft.getHistories().getLast()
                ));
        when(userApi.getUsersByIdsOrdered(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        when(engineeringChangeApi.getEngineeringChangeSnapshotMap(java.util.Set.of(engineeringChangeId))).thenReturn(Map.of(
                engineeringChangeId,
                new EngineeringChangeSnapshot(engineeringChangeId, 101, "상위품 개정", null)
        ));

        PartRevisionHistoryResult result = partQuery.getHistory(new PartRevisionHistoryCondition(part.getId()));

        assertEquals(2, result.items().size());
        assertEquals(PartRevisionReleaseWorkflowType.ENGINEERING_CHANGE, result.items().getFirst().releaseWorkflowType());
        assertEquals(engineeringChangeId, result.items().getFirst().releaseSourceId());
        assertEquals(101, result.items().getFirst().releaseSourceNumber());
        assertEquals("상위품 개정", result.items().getFirst().releaseSourceTitle());
        assertNull(result.items().getFirst().releaseReason());
        assertEquals("1", result.items().getLast().revisionCode());
        assertEquals("최초 반영", result.items().getLast().releaseReason());
        assertEquals(PartRevisionReleaseWorkflowType.DIRECT, result.items().getLast().releaseWorkflowType());
        assertEquals(2, result.items().getLast().drafts().size());
        assertEquals(PartRevisionCreationSourceType.USER, result.items().getLast().drafts().getFirst().creationSourceType());
        assertEquals("2", result.items().getLast().drafts().getFirst().releasedRevisionCode());
        assertEquals(PartRevisionStatus.CANCELED, result.items().getLast().drafts().getLast().status());
        assertEquals(PartRevisionCreationSourceType.USER, result.items().getLast().drafts().getLast().creationSourceType());
        assertNull(result.items().getLast().drafts().getLast().releasedRevisionCode());
        assertEquals("폐기", result.items().getLast().drafts().getLast().reason());
        assertEquals(PartRevisionStatus.RELEASED, result.items().getFirst().status());
    }

    @Test
    void getPreviewSources_DWG도면은_업로드가능해도_preview후보에는_포함되지_않는다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-150");
        PartRevision draft = PartRevision.createInitialDraft(part, "part", actorId);
        Drawing dwgDrawing = Drawing.create("DRW-1", "dwg 도면");
        File dwgFile = createUploadedFile("sample.dwg", 111L);

        dwgDrawing.assignPartRevision(draft.getId());
        dwgDrawing.assignSourceFile(dwgFile.getId(), DrawingSourceType.CAD_2D, DrawingDimension.TWO_D);
        dwgDrawing.changeOriginalFileKey(dwgFile.getFileKey());

        when(partRevisionRepository.findByIdAndPartId(draft.getId(), part.getId()))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(draft.getId()))
                .thenReturn(List.of(dwgDrawing));
        when(partPreviewRepository.findByPartRevisionId(draft.getId())).thenReturn(Optional.empty());
        when(fileRepository.findByIdAndDeletedAtIsNull(dwgFile.getId())).thenReturn(Optional.of(dwgFile));

        PartPreviewSourcesResult result = partQuery.getPreviewSources(
                new PartPreviewSourcesCondition(part.getId(), draft.getId())
        );

        assertEquals(0, result.total());
        assertEquals(0, result.items().size());
    }

    @Test
    void getDraft_원본_파일이_없는_도면은_files_count에_포함하지_않는다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-200");
        PartRevision draft = PartRevision.createInitialDraft(part, "part", actorId);
        UUID missingSourceFileId = UUID.randomUUID();

        Drawing danglingDrawing = Drawing.create("DRW-1", "도면");
        danglingDrawing.assignPartRevision(draft.getId());
        danglingDrawing.assignSourceFile(
                missingSourceFileId,
                com.fabbitinc.server.domain.drawing.model.DrawingSourceType.PDF_DOCUMENT,
                DrawingDimension.TWO_D
        );
        danglingDrawing.changeOriginalFileKey("tenants/org/missing.pdf");

        when(partRevisionRepository.findByIdAndPartId(draft.getId(), part.getId()))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
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

        PartDetailResult result = partQuery.get(new PartDetailCondition(part.getId(), draft.getId()));

        assertEquals(0L, result.filesCount());
    }

    @Test
    void getDraft_preview전용파일만_있으면_filesCount는_0이다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-201");
        PartRevision draft = PartRevision.createInitialDraft(part, "part", actorId);
        PartPreview partPreview = PartPreview.create(draft.getId());
        File previewFile = createUploadedFile("preview.step", 220L);
        var previewFileRelation = partPreview.addPreviewFile(previewFile.getId());
        setCreatedAt(previewFileRelation, Instant.parse("2026-03-18T00:00:00Z"));
        previewFile.assignOwner("part_preview_file", previewFileRelation.getId());
        partPreview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, previewFileRelation.getId(), DrawingDimension.THREE_D);

        when(partRevisionRepository.findByIdAndPartId(draft.getId(), part.getId()))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(partPreviewRepository.findByPartRevisionId(draft.getId())).thenReturn(Optional.of(partPreview));
        when(fileRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                "part_preview_file",
                previewFileRelation.getId()
        )).thenReturn(List.of(previewFile));
        when(partSupplierRepository.countByPartRevisionId(draft.getId())).thenReturn(0L);
        when(fileRepository.countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                "part_revision",
                draft.getId(),
                FileStatus.UPLOADED
        )).thenReturn(0L);
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(draft.getId()))
                .thenReturn(List.of());
        when(engineeringBomItemRepository.countByParentPartRevisionId(draft.getId())).thenReturn(0L);
        when(engineeringBomItemRepository.countByChildPartRevisionId(draft.getId())).thenReturn(0L);
        when(projectApi.countPartProjects(part.getId())).thenReturn(0L);

        PartDetailResult result = partQuery.get(new PartDetailCondition(part.getId(), draft.getId()));

        assertEquals(0L, result.filesCount());
    }

    @Test
    void get_상세응답은_revisionStatus와_baseRevision문맥을_포함한다() {
        UUID actorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Part part = Part.create("P-300", categoryId, com.fabbitinc.server.domain.part.model.PartItemType.MANUFACTURED);
        PartRevision baseRevision = PartRevision.createInitialDraft(part, "base", actorId);
        setCreatedAt(baseRevision, Instant.parse("2026-03-18T00:00:00Z"));
        baseRevision.release("1", actorId);
        PartRevision draft = PartRevision.createDraft(part, baseRevision.getId(), "draft", actorId);

        when(partRevisionRepository.findByIdAndPartId(draft.getId(), part.getId()))
                .thenReturn(Optional.of(draft));
        when(partRepository.findById(part.getId())).thenReturn(Optional.of(part));
        when(partRevisionRepository.findById(baseRevision.getId())).thenReturn(Optional.of(baseRevision));
        when(partPreviewRepository.findByPartRevisionId(draft.getId())).thenReturn(Optional.empty());
        when(partSupplierRepository.countByPartRevisionId(draft.getId())).thenReturn(0L);
        when(fileRepository.countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                "part_revision",
                draft.getId(),
                FileStatus.UPLOADED
        )).thenReturn(0L);
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(draft.getId()))
                .thenReturn(List.of());
        when(engineeringBomItemRepository.countByParentPartRevisionId(draft.getId())).thenReturn(0L);
        when(engineeringBomItemRepository.countByChildPartRevisionId(draft.getId())).thenReturn(0L);
        when(projectApi.countPartProjects(part.getId())).thenReturn(0L);
        when(partCategoryRepository.findById(categoryId)).thenReturn(Optional.of(
                com.fabbitinc.server.domain.part.model.PartCategory.create("기구", "MECH-", "", 4, true)
        ));

        PartDetailResult result = partQuery.get(new PartDetailCondition(part.getId(), draft.getId()));

        assertEquals(draft.getId(), result.revisionId());
        assertEquals(PartRevisionStatus.DRAFT, result.revisionStatus());
        assertEquals(categoryId, result.categoryId());
        assertEquals("기구", result.categoryName());
        assertEquals(baseRevision.getId(), result.baseRevisionId());
        assertEquals("1", result.baseRevisionCode());
    }

    @Test
    void list_목록응답은_revisionId와_revisionStatus를_포함한다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-301");
        PartRevision released = PartRevision.createInitialDraft(part, "released", actorId);
        released.release("1", actorId);
        part.assignCurrentReleasedRevision(released.getId());

        when(partRepository.findAllByOrderByPartNumberAsc(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(part));
        when(partRevisionRepository.findByPartIdInOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(released));
        when(drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(released.getId()))
                .thenReturn(List.of());
        when(engineeringBomItemRepository.countByParentPartRevisionId(released.getId())).thenReturn(0L);

        PartListResult result = partQuery.list(new PartListCondition(null, null, null, null, null, null, null, null, 20));

        assertEquals(1, result.items().size());
        assertEquals(released.getId(), result.items().getFirst().revisionId());
        assertEquals(PartRevisionStatus.RELEASED, result.items().getFirst().revisionStatus());
    }

    @Test
    void lookup_항목은_revisionId를_포함한다() {
        UUID actorId = UUID.randomUUID();
        Part part = Part.create("P-302");
        PartRevision released = PartRevision.createInitialDraft(part, "released", actorId);
        released.release("1", actorId);
        part.assignCurrentReleasedRevision(released.getId());

        when(partRepository.findAllByOrderByPartNumberAsc(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(part));
        when(partRevisionRepository.findByPartIdInOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(released));

        PartLookupResult result = partQuery.lookup(new com.fabbitinc.server.application.part.query.condition.PartLookupCondition(null, 10));

        assertEquals(1, result.items().size());
        assertEquals(part.getId(), result.items().getFirst().id());
        assertEquals(released.getId(), result.items().getFirst().revisionId());
    }

    @Test
    void getBom_항목은_partId_revisionId_revisionStatus를_포함한다() {
        UUID actorId = UUID.randomUUID();
        Part parentPart = Part.create("P-400");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "parent", actorId);
        parentRevision.release("1", actorId);

        Part childPart = Part.create("P-401");
        PartRevision childRevision = PartRevision.createInitialDraft(childPart, "child", actorId);
        childRevision.release("1", actorId);

        EngineeringBomItem bomItem = EngineeringBomItem.add(
                parentRevision.getId(),
                "1",
                childRevision.getId(),
                java.math.BigDecimal.ONE,
                "{}"
        );

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(partRepository.findById(parentPart.getId())).thenReturn(Optional.of(parentPart));
        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(parentRevision.getId()))
                .thenReturn(List.of(bomItem));
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(parentRevision.getId()))
                .thenReturn(List.of());
        when(partRevisionRepository.findAllById(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(childRevision));
        when(partRepository.findAllById(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(childPart));

        PartBomResult result = partQuery.get(new PartBomCondition(parentPart.getId(), parentRevision.getId()));

        assertEquals(1, result.children().size());
        assertEquals(bomItem.getId(), result.children().getFirst().bomItemId());
        assertEquals(childPart.getId(), result.children().getFirst().partId());
        assertEquals(childRevision.getId(), result.children().getFirst().revisionId());
        assertEquals(PartRevisionStatus.RELEASED, result.children().getFirst().revisionStatus());
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

    private void setCreatedAt(Object target, Instant createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
    }
}
