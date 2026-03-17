package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.mapping.api.MappingApi;
import com.fabbitinc.server.application.part.model.BomDirection;
import com.fabbitinc.server.application.part.model.DrawingViewerType;
import com.fabbitinc.server.application.part.model.PartAttachmentType;
import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartDraftLookupCondition;
import com.fabbitinc.server.application.part.query.condition.PartDraftDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.query.condition.PartInProgressListCondition;
import com.fabbitinc.server.application.part.query.condition.PartInProgressStatusFilter;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartLookupCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewSourcesCondition;
import com.fabbitinc.server.application.part.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionDiffCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionHistoryCondition;
import com.fabbitinc.server.application.part.query.condition.PartSuppliersCondition;
import com.fabbitinc.server.application.part.query.result.BomTreeResult;
import com.fabbitinc.server.application.part.query.result.CategoryLookupResult;
import com.fabbitinc.server.application.part.query.result.CategoryStatsResult;
import com.fabbitinc.server.application.part.query.result.PartBomResult;
import com.fabbitinc.server.application.part.query.result.PartDetailResult;
import com.fabbitinc.server.application.part.query.result.PartDraftLookupResult;
import com.fabbitinc.server.application.part.query.result.PartFilesResult;
import com.fabbitinc.server.application.part.query.result.PartFilterOptionsResult;
import com.fabbitinc.server.application.part.query.result.PartInProgressListResult;
import com.fabbitinc.server.application.part.query.result.PartListResult;
import com.fabbitinc.server.application.part.query.result.PartLookupResult;
import com.fabbitinc.server.application.part.query.result.PartPreviewResult;
import com.fabbitinc.server.application.part.query.result.PartPreviewSourcesResult;
import com.fabbitinc.server.application.part.query.result.PartProjectsResult;
import com.fabbitinc.server.application.part.query.result.PartRevisionDiffResult;
import com.fabbitinc.server.application.part.query.result.PartRevisionDiffSummaryResult;
import com.fabbitinc.server.application.part.query.result.PartRevisionHistoryResult;
import com.fabbitinc.server.application.part.query.result.PartSuppliersResult;
import com.fabbitinc.server.application.part.query.result.PartUserSummaryResult;
import com.fabbitinc.server.application.part.service.PartPreviewService;
import com.fabbitinc.server.application.project.api.ProjectApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingExtension;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewFile;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingJob;
import com.fabbitinc.server.domain.part.model.PartPreviewServingProjection;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingStatus;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionHistory;
import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.model.PartSupplier;
import com.fabbitinc.server.domain.part.repository.PartRevisionHistoryRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewFileRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewProcessingJobRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewRepository;
import com.fabbitinc.server.domain.part.repository.PartPreviewServingProjectionRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final MappingApi mappingApi;
    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final FileRepository fileRepository;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartSupplierRepository partSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final DrawingRepository drawingRepository;
    private final PartRevisionHistoryRepository partRevisionHistoryRepository;
    private final PartPreviewRepository partPreviewRepository;
    private final PartPreviewFileRepository partPreviewFileRepository;
    private final PartPreviewProcessingJobRepository partPreviewProcessingJobRepository;
    private final PartPreviewServingProjectionRepository partPreviewServingProjectionRepository;
    private final ProjectApi projectApi;
    private final UserApi userApi;
    private final FileUrlResolver fileUrlResolver;
    private final EntityManager entityManager;

    private static final Pattern STRING_PATTERN = Pattern.compile("^\"(.*)\"$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");
    private static final Pattern HISTORY_REASON_PATTERN = Pattern.compile("\"reason\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern HISTORY_IDENTIFIER_PATTERN = Pattern.compile("\"revisionCode\"\\s*:\\s*\"([^\"]*)\"");
    private static final int MAX_BOM_DEPTH = 30;
    private static final Comparator<ResolvedPart> PART_LIST_ORDER =
            Comparator.comparing(ResolvedPart::partNumber)
                    .thenComparing(ResolvedPart::id);
    private static final Comparator<ResolvedWorkItem> WORK_ITEM_ORDER =
            Comparator.comparing((ResolvedWorkItem item) -> item.revision().getUpdatedAt(), Comparator.reverseOrder())
                    .thenComparing(item -> item.revision().getId(), Comparator.reverseOrder());

    public PartLookupResult lookup(PartLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<Part> parts;
        if (condition.search() == null || condition.search().isBlank()) {
            parts = partRepository.findAllByOrderByPartNumberAsc(PageRequest.of(0, condition.limit()));
        } else {
            String normalizedSearch = condition.search().trim();
            List<PartRevision> matchedRevisions = partRevisionRepository
                    .findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAscCreatedAtDesc(
                    normalizedSearch,
                    normalizedSearch,
                    PageRequest.of(0, Math.max(condition.limit() * 5, condition.limit()))
            );
            LinkedHashMap<UUID, PartRevision> latestMatched = new LinkedHashMap<>();
            matchedRevisions.forEach(revision -> latestMatched.putIfAbsent(revision.getPartId(), revision));
            Map<UUID, Part> partsById = new LinkedHashMap<>();
            partRepository.findAllById(latestMatched.keySet())
                    .forEach(part -> partsById.put(part.getId(), part));
            parts = latestMatched.keySet().stream()
                    .map(partsById::get)
                    .filter(java.util.Objects::nonNull)
                    .limit(condition.limit())
                    .toList();
        }

        List<ResolvedPart> resolvedParts = resolveParts(parts);
        List<PartLookupResult.Item> items = resolvedParts.stream()
                .map(part -> new PartLookupResult.Item(part.id(), part.partNumber(), part.name()))
                .toList();
        return new PartLookupResult(items);
    }

    public PartDraftLookupResult lookupDrafts(PartDraftLookupCondition condition) {
        UUID actorId = currentAuthProvider.getCurrentAuth().userId();

        PathBuilder<PartRevision> revision = new PathBuilder<>(PartRevision.class, "revision");

        BooleanBuilder predicate = new BooleanBuilder()
                .and(revision.getEnum("status", PartRevisionStatus.class).eq(PartRevisionStatus.DRAFT))
                .and(revision.get("engineeringChangeId", UUID.class).isNull())
                .and(revision.get("createdBy", UUID.class).eq(actorId));

        if (condition.search() != null && !condition.search().isBlank()) {
            String keyword = condition.search().trim();
            predicate.and(
                    revision.getString("partNumber").containsIgnoreCase(keyword)
                            .or(revision.getString("name").containsIgnoreCase(keyword))
            );
        }

        List<PartRevision> drafts = queryFactory()
                .selectFrom(revision)
                .where(predicate)
                .orderBy(
                        revision.getString("partNumber").asc(),
                        revision.getDateTime("createdAt", java.time.Instant.class).desc()
                )
                .limit(condition.limit())
                .fetch();

        Map<UUID, PartRevision> baseRevisions = drafts.stream()
                .map(PartRevision::getBaseRevisionId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toSet(),
                        baseRevisionIds -> baseRevisionIds.isEmpty()
                                ? Map.<UUID, PartRevision>of()
                                : partRevisionRepository.findAllById(baseRevisionIds).stream()
                                .collect(java.util.stream.Collectors.toMap(PartRevision::getId, base -> base))
                ));

        Map<UUID, User> createdByUsers = drafts.stream()
                .map(PartRevision::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        userIds -> userIds.isEmpty()
                                ? Map.<UUID, User>of()
                                : userApi.getUsersByIdsOrdered(List.copyOf(userIds)).stream()
                                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user))
                ));

        return new PartDraftLookupResult(
                drafts.stream()
                        .map(draft -> {
                            PartRevision baseRevision = draft.getBaseRevisionId() == null
                                    ? null
                                    : baseRevisions.get(draft.getBaseRevisionId());
                            return new PartDraftLookupResult.Item(
                                    draft.getId(),
                                    draft.getPartId(),
                                    draft.getPartNumber(),
                                    baseRevision == null ? null : baseRevision.getRevisionCode(),
                                    draft.getDraftKey(),
                                    draft.getName(),
                                    toUserSummary(createdByUsers.get(draft.getCreatedBy()))
                            );
                        })
                        .toList()
        );
    }

    public CategoryStatsResult listCategories() {
        currentAuthProvider.getCurrentAuth();
        List<CategoryStatsResult.Item> items = partRevisionRepository.findDistinctCategories().stream()
                .map(category -> new CategoryStatsResult.Item(
                        category,
                        partRevisionRepository.countByCategory(category)
                ))
                .toList();
        return new CategoryStatsResult(items);
    }

    public CategoryLookupResult lookupCategories() {
        currentAuthProvider.getCurrentAuth();
        return new CategoryLookupResult(findDistinctCategories());
    }

    public PartFilterOptionsResult getFilterOptions() {
        currentAuthProvider.getCurrentAuth();
        return new PartFilterOptionsResult(
                findDistinctCategories(),
                findDistinctLifecycleStates()
        );
    }

    public PartListResult list(PartListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        PartLifecycleState lifecycleState = parseLifecycleState(condition.lifecycleState());
        if (condition.nextCursor() != null && condition.prevCursor() != null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "next_cursor와 prev_cursor는 동시에 사용할 수 없습니다"
            );
        }

        List<ResolvedPart> filtered = filterResolvedParts(
                resolveReleasedParts(findPartsForExport(
                        null,
                        condition.category(),
                        lifecycleState,
                        null,
                        null,
                        null,
                        condition.projectId()
                )),
                condition.search(),
                condition.category(),
                condition.hasChildren(),
                condition.hasDrawing()
        ).stream()
                .sorted(PART_LIST_ORDER)
                .toList();

        int fromIndex = resolvePartListStartIndex(filtered, condition);
        int toIndex = Math.min(fromIndex + condition.limit(), filtered.size());

        List<ResolvedPart> pageItems = filtered.subList(fromIndex, toIndex);
        List<PartListResult.Item> items = pageItems.stream()
                .map(part -> new PartListResult.Item(
                        part.id(),
                        part.partNumber(),
                        part.name(),
                        part.category(),
                        part.revisionCode(),
                        part.lifecycleState(),
                        countAttachedDrawings(part.revision()) > 0,
                        countEngineeringBomChildren(part.revision())
                ))
                .toList();

        String prevCursor = fromIndex > 0 && !pageItems.isEmpty()
                ? encodePartListCursor(pageItems.get(0))
                : null;
        String nextCursor = toIndex < filtered.size() && !pageItems.isEmpty()
                ? encodePartListCursor(pageItems.get(pageItems.size() - 1))
                : null;

        return new PartListResult(nextCursor, prevCursor, items);
    }

    public PartInProgressListResult listInProgress(PartInProgressListCondition condition) {
        UUID actorId = currentAuthProvider.getCurrentAuth().userId();
        if (condition.nextCursor() != null && condition.prevCursor() != null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "next_cursor와 prev_cursor는 동시에 사용할 수 없습니다"
            );
        }

        PartLifecycleState lifecycleState = parseLifecycleState(condition.lifecycleState());
        List<PartRevisionStatus> statuses = normalizeInProgressStatuses(condition.statuses());
        Map<UUID, PartRevision> baseRevisionsById = new HashMap<>();

        List<ResolvedWorkItem> filtered = filterResolvedWorkItems(
                resolveWorkItems(
                        findPartsForExport(
                                null,
                                condition.category(),
                                lifecycleState,
                                null,
                                null,
                                null,
                                condition.projectId()
                        ),
                        statuses,
                        baseRevisionsById
                ),
                actorId,
                condition.search(),
                condition.category(),
                condition.mineOnly(),
                condition.hasChildren(),
                condition.hasDrawing()
        ).stream()
                .sorted(WORK_ITEM_ORDER)
                .toList();

        int fromIndex = resolveWorkItemStartIndex(filtered, condition);
        int toIndex = Math.min(fromIndex + condition.limit(), filtered.size());
        List<ResolvedWorkItem> pageItems = filtered.subList(fromIndex, toIndex);

        List<PartInProgressListResult.Item> items = pageItems.stream()
                .map(item -> new PartInProgressListResult.Item(
                        item.part().getId(),
                        item.revision().getId(),
                        item.part().getPartNumber(),
                        item.revision().getName(),
                        item.revision().getCategory(),
                        item.revision().getStatus(),
                        item.revision().getRevisionCode(),
                        item.revision().getDraftKey(),
                        item.baseRevisionCode(),
                        item.part().getLifecycleState(),
                        countAttachedDrawings(item.revision()) > 0,
                        countEngineeringBomChildren(item.revision()),
                        item.revision().getUpdatedAt()
                ))
                .toList();

        String prevCursor = fromIndex > 0 && !pageItems.isEmpty()
                ? encodeWorkItemCursor(pageItems.get(0))
                : null;
        String nextCursor = toIndex < filtered.size() && !pageItems.isEmpty()
                ? encodeWorkItemCursor(pageItems.get(pageItems.size() - 1))
                : null;

        return new PartInProgressListResult(nextCursor, prevCursor, items);
    }

    public byte[] export(PartExportCondition condition) {
        currentAuthProvider.getCurrentAuth();
        PartLifecycleState lifecycleState = parseLifecycleState(condition.lifecycleState());
        List<ResolvedPart> parts = filterResolvedParts(
                resolveParts(findPartsForExport(
                        null,
                        condition.category(),
                        lifecycleState,
                        null,
                        null,
                        condition.partIds(),
                        condition.projectId()
                )),
                condition.search(),
                condition.category(),
                condition.hasChildren(),
                condition.hasDrawing()
        );

        Set<String> extKeys = new TreeSet<>();
        Map<UUID, Map<String, Object>> extValues = new HashMap<>();
        for (ResolvedPart part : parts) {
            Map<String, Object> parsed = parseExtendedProperties(part.extendedProperties());
            extValues.put(part.id(), parsed);
            extKeys.addAll(parsed.keySet());
        }

        List<String> columns = new ArrayList<>(List.of(
                "part_number",
                "name",
                "revision",
                "material",
                "unit",
                "description",
                "category",
                "is_phantom",
                "lifecycle_state",
                "lead_time_days"
        ));
        columns.addAll(extKeys);
        Map<String, String> headerAliases = mappingApi.getPartExportHeaderAliases(condition.mappingId());
        List<String> headers = columns.stream()
                .map(column -> headerAliases.getOrDefault(column, column))
                .toList();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("부품목록");
            writeHeader(sheet, headers);

            int rowIndex = 1;
            for (ResolvedPart part : parts) {
                Row row = sheet.createRow(rowIndex++);
                Map<String, Object> extended = extValues.getOrDefault(part.id(), Map.of());

                writeCell(row, 0, part.partNumber());
                writeCell(row, 1, part.name());
                writeCell(row, 2, part.revisionCode());
                writeCell(row, 3, part.material());
                writeCell(row, 4, part.unit());
                writeCell(row, 5, part.description());
                writeCell(row, 6, part.category());
                writeCell(row, 7, part.phantom());
                writeCell(row, 8, part.lifecycleState());
                writeCell(row, 9, part.leadTimeDays());

                int colIndex = 10;
                for (String key : extKeys) {
                    writeCell(row, colIndex++, extended.get(key));
                }
            }
            autoFitColumns(sheet, headers.size());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "엑셀 파일 생성에 실패했습니다");
        }
    }

    public byte[] exportBomTree(BomTreeExportCondition condition) {
        BomTreeResult tree = getBomTree(new BomTreeCondition(
                condition.partNumber(),
                condition.revisionCode(),
                condition.direction()
        ));
        List<BomFlatRow> rows = new ArrayList<>();
        flattenBomTree(tree.root(), 0, rows);

        List<String> headers = List.of(
                "level",
                "part_number",
                "name",
                "revision",
                "quantity",
                "material",
                "unit",
                "category",
                "lifecycle_state"
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BOM");
            writeHeader(sheet, headers);

            int rowIndex = 1;
            for (BomFlatRow item : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, item.level());
                writeCell(row, 1, item.partNumber());
                writeCell(row, 2, item.name());
                writeCell(row, 3, item.revision());
                writeCell(row, 4, item.quantity());
                writeCell(row, 5, item.material());
                writeCell(row, 6, item.unit());
                writeCell(row, 7, item.category());
                writeCell(row, 8, item.lifecycleState());
            }

            autoFitColumns(sheet, headers.size());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "엑셀 파일 생성에 실패했습니다");
        }
    }

    public PartDetailResult get(PartDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();
        ResolvedPart resolvedPart = resolveRequiredPart(condition.partNumber(), condition.revisionCode());
        return buildPartDetailResult(resolvedPart);
    }

    public PartDetailResult getDraft(PartDraftDetailCondition condition) {
        currentAuthProvider.getCurrentAuth();
        ResolvedPart resolvedPart = resolveRequiredDraft(
                condition.partNumber(),
                condition.baseRevisionCode(),
                condition.draftKey()
        );
        return buildPartDetailResult(resolvedPart);
    }

    public PartRevisionHistoryResult getHistory(PartRevisionHistoryCondition condition) {
        currentAuthProvider.getCurrentAuth();

        Part part = partRepository.findByPartNumber(condition.partNumber())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(condition.partNumber())
                ));

        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId()).stream()
                .toList();
        if (revisions.isEmpty()) {
            return new PartRevisionHistoryResult(List.of());
        }

        Map<UUID, List<PartRevisionHistory>> historiesByRevisionId = groupHistoriesByRevisionId(revisions);
        Map<UUID, User> usersById = loadUsersByRevisionHistory(revisions, historiesByRevisionId);
        List<PartRevision> officialRevisions = revisions.stream()
                .filter(this::isOfficialRevision)
                .sorted(Comparator
                        .comparing(
                                (PartRevision revision) -> resolveReleaseOccurredAt(revision, historiesByRevisionId),
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .reversed()
                        .thenComparing(PartRevision::getCreatedAt, Comparator.reverseOrder()))
                .toList();
        if (officialRevisions.isEmpty()) {
            return new PartRevisionHistoryResult(List.of());
        }

        Map<UUID, List<File>> filesByRevisionId = groupFilesByRevisionId(officialRevisions);
        Map<UUID, List<Drawing>> drawingsByRevisionId = groupDrawingsByRevisionId(officialRevisions);
        Map<UUID, File> drawingSourceFilesById = loadDrawingSourceFiles(drawingsByRevisionId);
        Map<UUID, List<EngineeringBomItem>> bomItemsByRevisionId = groupBomItemsByRevisionId(officialRevisions);
        Map<UUID, List<PartRevision>> draftsByBaseRevisionId = revisions.stream()
                .filter(revision -> revision.getBaseRevisionId() != null)
                .collect(java.util.stream.Collectors.groupingBy(PartRevision::getBaseRevisionId));

        List<PartRevisionHistoryResult.Card> items = new ArrayList<>();
        for (int index = 0; index < officialRevisions.size(); index++) {
            PartRevision revision = officialRevisions.get(index);
            PartRevision previousRevision = index + 1 < officialRevisions.size() ? officialRevisions.get(index + 1) : null;
            RevisionDiffSnapshot diff = previousRevision == null
                    ? null
                    : buildRevisionDiffSnapshot(
                            previousRevision,
                            revision,
                            filesByRevisionId,
                            drawingsByRevisionId,
                            drawingSourceFilesById,
                            bomItemsByRevisionId,
                            usersById
                    );

            PartRevisionHistory history = findHistoryByAction(
                    historiesByRevisionId.getOrDefault(revision.getId(), List.of()),
                    PartRevisionHistoryActionType.RELEASED
            );

            items.add(new PartRevisionHistoryResult.Card(
                    revision.getId(),
                    revision.getRevisionCode(),
                    revision.getStatus(),
                    revision.getName(),
                    history == null ? revision.getCreatedAt() : history.getOccurredAt(),
                    history == null ? toUserSummary(usersById.get(revision.getCreatedBy())) : toUserSummary(usersById.get(history.getActorId())),
                    diff == null ? null : diff.summary(),
                    draftsByBaseRevisionId.getOrDefault(revision.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(PartRevision::getCreatedAt, Comparator.reverseOrder()))
                            .map(draft -> toHistoryDraft(draft, historiesByRevisionId.getOrDefault(draft.getId(), List.of()), usersById))
                            .toList()
            ));
        }

        return new PartRevisionHistoryResult(items);
    }

    public PartRevisionDiffResult getDiff(PartRevisionDiffCondition condition) {
        currentAuthProvider.getCurrentAuth();

        ResolvedPart target = resolveRequiredPart(condition.partNumber(), condition.revisionCode());
        PartRevision targetRevision = target.revision();
        PartRevision baseRevision = resolveBaseRevision(targetRevision, condition.baseRevisionCode());
        if (baseRevision == null) {
            throw new AppException(
                    ErrorCode.NOT_FOUND,
                    "비교 기준 이전 리비전을 찾을 수 없습니다: %s/%s".formatted(condition.partNumber(), condition.revisionCode())
            );
        }

        List<PartRevision> pair = List.of(baseRevision, targetRevision);
        Map<UUID, List<File>> filesByRevisionId = groupFilesByRevisionId(pair);
        Map<UUID, List<Drawing>> drawingsByRevisionId = groupDrawingsByRevisionId(pair);
        Map<UUID, File> drawingSourceFilesById = loadDrawingSourceFiles(drawingsByRevisionId);
        Map<UUID, List<EngineeringBomItem>> bomItemsByRevisionId = groupBomItemsByRevisionId(pair);
        Map<UUID, User> usersById = loadUsersByRevisionHistory(pair, Map.of());

        RevisionDiffSnapshot diff = buildRevisionDiffSnapshot(
                baseRevision,
                targetRevision,
                filesByRevisionId,
                drawingsByRevisionId,
                drawingSourceFilesById,
                bomItemsByRevisionId,
                usersById
        );

        return new PartRevisionDiffResult(
                new PartRevisionDiffResult.Revision(
                        baseRevision.getId(),
                        baseRevision.getRevisionCode(),
                        baseRevision.getStatus(),
                        baseRevision.getCreatedAt(),
                        toUserSummary(usersById.get(baseRevision.getCreatedBy()))
                ),
                new PartRevisionDiffResult.Revision(
                        targetRevision.getId(),
                        targetRevision.getRevisionCode(),
                        targetRevision.getStatus(),
                        targetRevision.getCreatedAt(),
                        toUserSummary(usersById.get(targetRevision.getCreatedBy()))
                ),
                diff.summary(),
                diff.attributes(),
                diff.files(),
                diff.bom()
        );
    }

    private PartDetailResult buildPartDetailResult(ResolvedPart resolvedPart) {
        Part part = resolvedPart.part();
        PartRevision revision = resolvedPart.revision();

        PartPreviewResult preview = loadPreview(revision);
        RevisionWorkflowCounts workflowCounts = countRevisionWorkflows(part.getId());

        long childrenCount = countEngineeringBomChildren(revision);
        long parentsCount = countEngineeringBomParents(revision);
        long suppliersCount = resolvedPart.revision() == null
                ? 0L
                : partSupplierRepository.countByPartRevisionId(revision.getId());
        long partFilesCount = revision == null
                ? 0L
                : fileRepository.countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                        "part_revision",
                        revision.getId(),
                        FileStatus.UPLOADED
                );
        long drawingsCount = countAttachedDrawings(revision);
        long projectsCount = projectApi.countPartProjects(part.getId());

        return new PartDetailResult(
                part.getId(),
                revision == null ? null : revision.getId(),
                part.getPartNumber(),
                resolvedPart.name(),
                resolvedPart.revisionCode(),
                resolvedPart.draftKey(),
                resolvedPart.material(),
                resolvedPart.unit(),
                resolvedPart.description(),
                resolvedPart.category(),
                part.getLifecycleState(),
                resolvedPart.phantom(),
                resolvedPart.leadTimeDays(),
                parseExtendedProperties(resolvedPart.extendedProperties()),
                preview,
                workflowCounts.draftCount(),
                childrenCount,
                parentsCount,
                suppliersCount,
                partFilesCount + drawingsCount,
                projectsCount
        );
    }

    public PartProjectsResult get(PartProjectsCondition condition) {
        currentAuthProvider.getCurrentAuth();
        var result = projectApi.listPartProjects(resolveRequiredPartId(condition.partNumber(), condition.revisionCode()));
        return new PartProjectsResult(
                result.total(),
                result.items().stream()
                        .map(item -> new PartProjectsResult.Item(item.id(), item.name(), item.description()))
                        .toList()
        );
    }

    public PartBomResult get(PartBomCondition condition) {
        currentAuthProvider.getCurrentAuth();
        ResolvedPart resolvedPart = resolveRequiredPart(condition.partNumber(), condition.revisionCode());
        PartRevision revision = resolvedPart.revision();
        if (revision == null) {
            return new PartBomResult(List.of(), List.of());
        }

        List<EngineeringBomItem> childItems = engineeringBomItemRepository
                .findByParentPartRevisionIdOrderByCreatedAtAsc(revision.getId());
        List<EngineeringBomItem> parentItems = engineeringBomItemRepository
                .findByChildPartRevisionIdOrderByCreatedAtAsc(revision.getId());

        Map<UUID, PartRevision> relatedRevisions = loadPartRevisions(
                collectRelatedRevisionIds(childItems, parentItems, revision.getId())
        );
        Map<UUID, Part> relatedParts = loadPartsByRevisionIds(relatedRevisions.values());

        List<PartBomResult.Child> children = childItems.stream()
                .map(item -> {
                    PartRevision childRevision = relatedRevisions.get(item.getChildPartRevisionId());
                    Part childPart = childRevision == null ? null : relatedParts.get(childRevision.getPartId());
                    return new PartBomResult.Child(
                            childPart == null ? null : childPart.getId(),
                            childPart == null ? null : childPart.getPartNumber(),
                            resolveName(childRevision),
                            resolveRevisionCode(childRevision),
                            item.getLineNumber(),
                            item.getQuantity(),
                            parseExtendedProperties(item.getExtendedProperties())
                    );
                })
                .sorted((left, right) -> {
                    int lineNumberCompare = compareLineNumbers(left.lineNumber(), right.lineNumber());
                    if (lineNumberCompare != 0) {
                        return lineNumberCompare;
                    }
                    return Comparator.nullsLast(String::compareTo).compare(left.partNumber(), right.partNumber());
                })
                .toList();

        List<PartBomResult.Parent> parents = parentItems.stream()
                .map(item -> {
                    PartRevision parentRevision = relatedRevisions.get(item.getParentPartRevisionId());
                    Part parentPart = parentRevision == null ? null : relatedParts.get(parentRevision.getPartId());
                    return new PartBomResult.Parent(
                            parentPart == null ? null : parentPart.getId(),
                            parentPart == null ? null : parentPart.getPartNumber(),
                            resolveName(parentRevision),
                            resolveRevisionCode(parentRevision),
                            item.getLineNumber(),
                            item.getQuantity(),
                            parseExtendedProperties(item.getExtendedProperties())
                    );
                })
                .sorted((left, right) -> {
                    int lineNumberCompare = compareLineNumbers(left.lineNumber(), right.lineNumber());
                    if (lineNumberCompare != 0) {
                        return lineNumberCompare;
                    }
                    return Comparator.nullsLast(String::compareTo).compare(left.partNumber(), right.partNumber());
                })
                .toList();

        return new PartBomResult(children, parents);
    }

    public BomTreeResult getBomTree(BomTreeCondition condition) {
        currentAuthProvider.getCurrentAuth();
        ResolvedPart requestedRoot = resolveRequiredPart(condition.partNumber(), condition.revisionCode());
        PartRevision requestedRevision = requestedRoot.revision();
        if (requestedRevision == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "PartRevision '%s/%s'을(를) 찾을 수 없습니다"
                    .formatted(condition.partNumber(), condition.revisionCode()));
        }

        BomDirection resolvedDirection = parseBomDirection(condition.direction());
        boolean reverse = resolvedDirection == BomDirection.REVERSE;
        List<BomEdge> edges = fetchBomEdges(requestedRevision.getId(), reverse);

        Set<UUID> revisionIds = collectRevisionIds(edges, requestedRevision.getId());
        Map<UUID, PartRevision> revisionsById = loadPartRevisions(revisionIds);
        Map<UUID, Part> partsById = loadPartsByRevisionIds(revisionsById.values());
        BomTreeResult.Node root = buildBomTree(
                requestedRevision.getId(),
                edges,
                revisionsById,
                partsById,
                reverse,
                BigDecimal.ONE
        );

        return new BomTreeResult(root, resolvedDirection, revisionIds.size());
    }

    public PartFilesResult get(PartFilesCondition condition) {
        currentAuthProvider.getCurrentAuth();
        ResolvedPart resolvedPart = resolveRequiredTarget(
                condition.partNumber(),
                condition.revisionCode(),
                condition.baseRevisionCode(),
                condition.draftKey()
        );
        PartRevision revision = resolvedPart.revision();

        List<PartFilesResult.Item> items = new ArrayList<>();
        if (revision != null) {
            fileRepository.findByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                            "part_revision",
                            revision.getId(),
                            FileStatus.UPLOADED
                    ).stream()
                    .map(this::toPartAttachmentItem)
                    .forEach(items::add);
        }

        findAttachedDrawings(revision).stream()
                .map(this::toDrawingAttachmentItem)
                .filter(java.util.Objects::nonNull)
                .forEach(items::add);

        items.sort(Comparator.comparing(PartFilesResult.Item::createdAt).reversed());

        return new PartFilesResult(items.size(), items);
    }

    public PartPreviewSourcesResult getPreviewSources(PartPreviewSourcesCondition condition) {
        currentAuthProvider.getCurrentAuth();
        ResolvedPart resolvedPart = resolveRequiredTarget(
                condition.partNumber(),
                condition.revisionCode(),
                condition.baseRevisionCode(),
                condition.draftKey()
        );
        PartRevision revision = resolvedPart.revision();
        ActivePreviewSource activePreviewSource = loadActivePreviewSource(revision);

        List<PartPreviewSourcesResult.Item> items = new ArrayList<>();
        findAttachedDrawings(revision).stream()
                .map(drawing -> toDrawingPreviewSourceItem(drawing, activePreviewSource))
                .filter(java.util.Objects::nonNull)
                .forEach(items::add);

        findPreviewFiles(revision).stream()
                .map(previewFile -> toPreviewFileSourceItem(previewFile, activePreviewSource))
                .filter(java.util.Objects::nonNull)
                .forEach(items::add);

        items.sort(Comparator.comparing(PartPreviewSourcesResult.Item::createdAt).reversed());
        return new PartPreviewSourcesResult(items.size(), items);
    }

    public List<PartFilesResult.Item> getFiles(FileItemsCondition condition) {
        currentAuthProvider.getCurrentAuth();
        return fileRepository.findByIdIn(condition.fileIds()).stream()
                .map(file -> new PartFilesResult.Item(
                        PartAttachmentType.FILE,
                        file.getId(),
                        null,
                        file.getOriginalName(),
                        file.getContentType(),
                        file.getFileSize(),
                        fileUrlResolver.resolve(file.getFileKey()),
                        file.getCreatedAt()
                ))
                .toList();
    }

    public PartSuppliersResult get(PartSuppliersCondition condition) {
        currentAuthProvider.getCurrentAuth();
        PartRevision revision = partRevisionRepository.findByPartNumberAndRevisionCode(
                        condition.partNumber(),
                        condition.revisionCode()
                )
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다"
                                .formatted(condition.partNumber(), condition.revisionCode())
                ));

        List<PartSupplier> links = partSupplierRepository.findByPartRevisionId(revision.getId());
        if (links.isEmpty()) {
            return new PartSuppliersResult(0, List.of());
        }

        Map<UUID, PartSupplier> linkMap = links.stream()
                .collect(java.util.stream.Collectors.toMap(PartSupplier::getSupplierId, link -> link));

        List<UUID> supplierIds = links.stream().map(PartSupplier::getSupplierId).toList();
        List<PartSuppliersResult.Item> items = supplierRepository.findAllById(supplierIds).stream()
                .map(supplier -> toRelatedSupplier(supplier, linkMap.get(supplier.getId())))
                .sorted(Comparator.comparing(PartSuppliersResult.Item::companyName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new PartSuppliersResult(items.size(), items);
    }

    private PartFilesResult.Item toPartAttachmentItem(File file) {
        return new PartFilesResult.Item(
                PartAttachmentType.FILE,
                file.getId(),
                null,
                file.getOriginalName(),
                file.getContentType(),
                file.getFileSize(),
                fileUrlResolver.resolve(file.getFileKey()),
                file.getCreatedAt()
        );
    }

    private PartFilesResult.Item toDrawingAttachmentItem(Drawing drawing) {
        File sourceFile = resolveDrawingFile(drawing);
        if (sourceFile == null || sourceFile.getDeletedAt() != null) {
            return null;
        }
        return new PartFilesResult.Item(
                PartAttachmentType.DRAWING,
                sourceFile.getId(),
                drawing.getId(),
                sourceFile.getOriginalName(),
                sourceFile.getContentType(),
                sourceFile.getFileSize(),
                fileUrlResolver.resolve(sourceFile.getFileKey()),
                drawing.getCreatedAt()
        );
    }

    private PartPreviewSourcesResult.Item toDrawingPreviewSourceItem(
            Drawing drawing,
            ActivePreviewSource activePreviewSource
    ) {
        File sourceFile = resolveDrawingFile(drawing);
        if (sourceFile == null || sourceFile.getDeletedAt() != null || !isPreviewSelectable(sourceFile)) {
            return null;
        }
        return new PartPreviewSourcesResult.Item(
                PartAttachmentType.DRAWING,
                PartPreviewSourceType.DRAWING,
                drawing.getId(),
                sourceFile.getId(),
                drawing.getId(),
                sourceFile.getOriginalName(),
                sourceFile.getContentType(),
                sourceFile.getFileSize(),
                fileUrlResolver.resolve(sourceFile.getFileKey()),
                activePreviewSource.sourceType() == PartPreviewSourceType.DRAWING
                        && drawing.getId().equals(activePreviewSource.sourceId()),
                false,
                drawing.getCreatedAt()
        );
    }

    private PartPreviewSourcesResult.Item toPreviewFileSourceItem(
            PartPreviewFile previewFile,
            ActivePreviewSource activePreviewSource
    ) {
        File file = fileRepository.findByIdAndOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                previewFile.getFileId(),
                PartPreviewService.OWNER_TYPE_PREVIEW_FILE,
                previewFile.getId()
        ).orElse(null);
        if (file == null) {
            return null;
        }
        return new PartPreviewSourcesResult.Item(
                PartAttachmentType.PREVIEW_FILE,
                PartPreviewSourceType.PREVIEW_FILE,
                previewFile.getId(),
                file.getId(),
                null,
                file.getOriginalName(),
                file.getContentType(),
                file.getFileSize(),
                fileUrlResolver.resolve(file.getFileKey()),
                activePreviewSource.sourceType() == PartPreviewSourceType.PREVIEW_FILE
                        && previewFile.getId().equals(activePreviewSource.sourceId()),
                true,
                previewFile.getCreatedAt()
        );
    }

    private PartUserSummaryResult toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new PartUserSummaryResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private PartPreviewResult toPartPreview(PartPreview partPreview, PartPreviewServingProjection projection) {
        if (partPreview == null || !partPreview.hasSource()) {
            return null;
        }
        String previewKey = projection == null ? partPreview.getWebpKey() : projection.getWebpKey();
        String pdfKey = projection == null ? partPreview.getPdfKey() : projection.getPdfKey();
        String glbKey = projection == null ? partPreview.getGlbKey() : projection.getGlbKey();
        String originalFileKey = resolvePreviewOriginalFileKey(partPreview);
        DrawingViewerType viewerType = resolvePreviewViewerType(partPreview, pdfKey, glbKey);
        String viewerKey = viewerType == DrawingViewerType.GLB ? glbKey : pdfKey;
        return new PartPreviewResult(
                partPreview.getId(),
                partPreview.getSourceType(),
                partPreview.getSourceId(),
                resolvePreviewProcessingStatus(partPreview),
                viewerType,
                fileUrlResolver.resolve(viewerKey),
                fileUrlResolver.resolve(previewKey),
                fileUrlResolver.resolve(originalFileKey)
        );
    }

    private PartPreviewProcessingStatus resolvePreviewProcessingStatus(PartPreview partPreview) {
        PartPreviewProcessingJob latestJob = partPreviewProcessingJobRepository
                .findFirstByPartPreviewIdOrderByCreatedAtDesc(partPreview.getId())
                .orElse(null);
        if (latestJob == null || latestJob.getStatus() == null) {
            return partPreview.getProcessingStatus();
        }
        return switch (latestJob.getStatus()) {
            case REQUESTED -> PartPreviewProcessingStatus.PENDING;
            case PROCESSING -> PartPreviewProcessingStatus.PROCESSING;
            case COMPLETED -> PartPreviewProcessingStatus.COMPLETED;
            case FAILED -> PartPreviewProcessingStatus.FAILED;
        };
    }

    private DrawingViewerType resolvePreviewViewerType(PartPreview partPreview, String pdfKey, String glbKey) {
        if (glbKey != null) {
            return DrawingViewerType.GLB;
        }
        if (pdfKey != null) {
            return DrawingViewerType.PDF;
        }
        return partPreview.getDimension() == DrawingDimension.THREE_D
                ? DrawingViewerType.GLB
                : DrawingViewerType.PDF;
    }

    private PartSuppliersResult.Item toRelatedSupplier(Supplier supplier, PartSupplier link) {
        return new PartSuppliersResult.Item(
                supplier.getId(),
                supplier.getCompanyName(),
                supplier.getCode(),
                supplier.getCountry(),
                link == null ? null : link.getUnitCost()
        );
    }

    private PartPreviewResult loadPreview(PartRevision revision) {
        if (revision == null) {
            return null;
        }
        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(revision.getId()).orElse(null);
        if (partPreview != null && partPreview.hasSource()) {
            PartPreviewServingProjection projection = partPreviewServingProjectionRepository.findById(partPreview.getId())
                    .orElse(null);
            return toPartPreview(partPreview, projection);
        }
        return null;
    }

    private ActivePreviewSource loadActivePreviewSource(PartRevision revision) {
        if (revision == null) {
            return new ActivePreviewSource(null, null);
        }
        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(revision.getId()).orElse(null);
        if (partPreview != null && partPreview.hasSource()) {
            return new ActivePreviewSource(partPreview.getSourceType(), partPreview.getSourceId());
        }
        return new ActivePreviewSource(null, null);
    }

    private List<Drawing> findAttachedDrawings(PartRevision revision) {
        if (revision == null) {
            return List.of();
        }
        LinkedHashMap<UUID, Drawing> drawings = new LinkedHashMap<>();
        drawingRepository.findByPartRevisionIdAndDeletedAtIsNullOrderByCreatedAtDesc(revision.getId())
                .forEach(drawing -> drawings.put(drawing.getId(), drawing));
        return List.copyOf(drawings.values());
    }

    private long countAttachedDrawings(PartRevision revision) {
        return findAttachedDrawings(revision).stream()
                .map(this::toDrawingAttachmentItem)
                .filter(java.util.Objects::nonNull)
                .count();
    }

    private List<PartPreviewFile> findPreviewFiles(PartRevision revision) {
        if (revision == null) {
            return List.of();
        }
        PartPreview partPreview = partPreviewRepository.findByPartRevisionId(revision.getId()).orElse(null);
        if (partPreview == null) {
            return List.of();
        }
        return partPreviewFileRepository.findByPartPreview_IdOrderByCreatedAtDesc(partPreview.getId());
    }

    private String resolvePreviewOriginalFileKey(PartPreview partPreview) {
        if (partPreview.getSourceType() == PartPreviewSourceType.DRAWING) {
            Drawing drawing = drawingRepository.findById(partPreview.getSourceId())
                    .filter(it -> it.getDeletedAt() == null)
                    .orElse(null);
            File sourceFile = resolveDrawingFile(drawing);
            return sourceFile == null ? null : sourceFile.getFileKey();
        }
        if (partPreview.getSourceType() == PartPreviewSourceType.PREVIEW_FILE) {
            return fileRepository.findByOwnerTypeAndOwnerIdAndDeletedAtIsNull(
                            PartPreviewService.OWNER_TYPE_PREVIEW_FILE,
                            partPreview.getSourceId()
                    ).stream()
                    .findFirst()
                    .map(File::getFileKey)
                    .orElse(null);
        }
        return null;
    }

    private File resolveDrawingFile(Drawing drawing) {
        if (drawing == null) {
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

    private boolean isPreviewSelectable(File file) {
        if (file == null || file.getDeletedAt() != null || file.getStatus() != FileStatus.UPLOADED) {
            return false;
        }
        return DrawingExtension.fromFileName(file.getOriginalName())
                .map(DrawingExtension::canStartPipelineDirectly)
                .orElse(false);
    }

    private List<BomEdge> fetchBomEdges(UUID rootRevisionId, boolean reverse) {
        String sql = reverse
                ? """
                        with recursive bom_cte as (
                            select
                                e.parent_part_revision_id as parent_revision_id,
                                e.child_part_revision_id as child_revision_id,
                                e.line_number as line_number,
                                e.quantity as quantity,
                                e.parent_part_revision_id as next_id,
                                1 as depth
                            from engineering_bom_items e
                            where e.child_part_revision_id = :rootRevisionId

                            union all

                            select
                                e.parent_part_revision_id as parent_revision_id,
                                e.child_part_revision_id as child_revision_id,
                                e.line_number as line_number,
                                e.quantity as quantity,
                                e.parent_part_revision_id as next_id,
                                bc.depth + 1 as depth
                            from bom_cte bc
                            join engineering_bom_items e on e.child_part_revision_id = bc.next_id
                            where bc.depth < :maxDepth
                        )
                        select parent_revision_id, child_revision_id, line_number, quantity from bom_cte
                        """
                : """
                        with recursive bom_cte as (
                            select
                                e.parent_part_revision_id as parent_revision_id,
                                e.child_part_revision_id as child_revision_id,
                                e.line_number as line_number,
                                e.quantity as quantity,
                                e.child_part_revision_id as next_id,
                                1 as depth
                            from engineering_bom_items e
                            where e.parent_part_revision_id = :rootRevisionId

                            union all

                            select
                                e.parent_part_revision_id as parent_revision_id,
                                e.child_part_revision_id as child_revision_id,
                                e.line_number as line_number,
                                e.quantity as quantity,
                                e.child_part_revision_id as next_id,
                                bc.depth + 1 as depth
                            from bom_cte bc
                            join engineering_bom_items e on e.parent_part_revision_id = bc.next_id
                            where bc.depth < :maxDepth
                        )
                        select parent_revision_id, child_revision_id, line_number, quantity from bom_cte
                        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("rootRevisionId", rootRevisionId);
        query.setParameter("maxDepth", MAX_BOM_DEPTH);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new BomEdge(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[2],
                        row[3] == null ? BigDecimal.ONE : toBigDecimal((Number) row[3])
                ))
                .toList();
    }

    private Set<UUID> collectRevisionIds(List<BomEdge> edges, UUID rootRevisionId) {
        Set<UUID> revisionIds = new HashSet<>();
        revisionIds.add(rootRevisionId);
        for (BomEdge edge : edges) {
            revisionIds.add(edge.parentRevisionId());
            revisionIds.add(edge.childRevisionId());
        }
        return revisionIds;
    }

    private BomTreeResult.Node buildBomTree(
            UUID rootRevisionId,
            List<BomEdge> edges,
            Map<UUID, PartRevision> revisionsById,
            Map<UUID, Part> partsById,
            boolean reverse,
            BigDecimal quantity
    ) {
        Map<UUID, List<BomEdge>> adjacency = new LinkedHashMap<>();
        for (BomEdge edge : edges) {
            UUID key = reverse ? edge.childRevisionId() : edge.parentRevisionId();
            adjacency.computeIfAbsent(key, ignored -> new ArrayList<>()).add(edge);
        }
        return buildBomTreeNode(
                rootRevisionId,
                adjacency,
                revisionsById,
                partsById,
                reverse,
                quantity,
                new ArrayList<>()
        );
    }

    private BomTreeResult.Node buildBomTreeNode(
            UUID revisionId,
            Map<UUID, List<BomEdge>> adjacency,
            Map<UUID, PartRevision> revisionsById,
            Map<UUID, Part> partsById,
            boolean reverse,
            BigDecimal quantity,
            List<UUID> ancestors
    ) {
        PartRevision revision = revisionsById.get(revisionId);
        Part part = revision == null ? null : partsById.get(revision.getPartId());

        List<UUID> nextAncestors = new ArrayList<>(ancestors);
        nextAncestors.add(revisionId);

        List<BomTreeResult.Node> children = adjacency.getOrDefault(revisionId, List.of()).stream()
                .sorted((left, right) -> compareLineNumbers(left.lineNumber(), right.lineNumber()))
                .filter(edge -> {
                    UUID nextRevisionId = reverse ? edge.parentRevisionId() : edge.childRevisionId();
                    return !nextAncestors.contains(nextRevisionId);
                })
                .map(edge -> buildBomTreeNode(
                        reverse ? edge.parentRevisionId() : edge.childRevisionId(),
                        adjacency,
                        revisionsById,
                        partsById,
                        reverse,
                        edge.quantity(),
                        nextAncestors
                ))
                .toList();

        if (part == null || revision == null) {
            return new BomTreeResult.Node(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    quantity,
                    children
            );
        }

        return new BomTreeResult.Node(
                part.getId(),
                part.getPartNumber(),
                resolveName(revision),
                resolveRevisionCode(revision),
                revision == null ? null : revision.getMaterial(),
                revision == null ? null : revision.getUnit(),
                revision == null ? null : revision.getCategory(),
                part.getLifecycleState(),
                quantity,
                children
        );
    }

    private List<Part> findPartsForExport(
            String search,
            String category,
            PartLifecycleState lifecycleState,
            Boolean ignoredHasDrawing,
            Boolean ignoredHasChildren,
            List<UUID> partIds,
            UUID projectId
    ) {
        PathBuilder<Part> part = new PathBuilder<>(Part.class, "part");
        PathBuilder<ProjectPart> projectPart = new PathBuilder<>(ProjectPart.class, "projectPart");

        BooleanBuilder predicate = buildPartPredicate(
                part,
                projectPart,
                search,
                category,
                lifecycleState,
                partIds,
                projectId
        );

        return queryFactory()
                .selectFrom(part)
                .where(predicate)
                .orderBy(part.getString("partNumber").asc())
                .fetch();
    }

    private int resolvePartListStartIndex(List<ResolvedPart> filtered, PartListCondition condition) {
        if (filtered.isEmpty()) {
            return 0;
        }
        if (condition.nextCursor() != null && !condition.nextCursor().isBlank()) {
            PartListCursor cursor = decodePartListCursor(condition.nextCursor());
            return findNextStartIndex(filtered, cursor);
        }
        if (condition.prevCursor() != null && !condition.prevCursor().isBlank()) {
            PartListCursor cursor = decodePartListCursor(condition.prevCursor());
            return findPreviousStartIndex(filtered, cursor, condition.limit());
        }
        return 0;
    }

    private int findNextStartIndex(List<ResolvedPart> filtered, PartListCursor cursor) {
        for (int i = 0; i < filtered.size(); i++) {
            if (comparePartListCursor(filtered.get(i), cursor) > 0) {
                return i;
            }
        }
        return filtered.size();
    }

    private int findPreviousStartIndex(List<ResolvedPart> filtered, PartListCursor cursor, int limit) {
        int endExclusive = filtered.size();
        for (int i = 0; i < filtered.size(); i++) {
            if (comparePartListCursor(filtered.get(i), cursor) >= 0) {
                endExclusive = i;
                break;
            }
        }
        return Math.max(0, endExclusive - limit);
    }

    private int comparePartListCursor(ResolvedPart part, PartListCursor cursor) {
        int partNumberCompare = part.partNumber().compareTo(cursor.partNumber());
        if (partNumberCompare != 0) {
            return partNumberCompare;
        }
        return part.id().compareTo(cursor.partId());
    }

    private String encodePartListCursor(ResolvedPart part) {
        String raw = part.partNumber() + "|" + part.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private PartListCursor decodePartListCursor(String encodedCursor) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            int delimiterIndex = raw.lastIndexOf('|');
            if (delimiterIndex <= 0 || delimiterIndex == raw.length() - 1) {
                throw new IllegalArgumentException("invalid cursor format");
            }
            String partNumber = raw.substring(0, delimiterIndex);
            UUID partId = UUID.fromString(raw.substring(delimiterIndex + 1));
            return new PartListCursor(partNumber, partId);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "cursor 값이 올바르지 않습니다");
        }
    }

    private int resolveWorkItemStartIndex(List<ResolvedWorkItem> filtered, PartInProgressListCondition condition) {
        if (filtered.isEmpty()) {
            return 0;
        }
        if (condition.nextCursor() != null && !condition.nextCursor().isBlank()) {
            UUID cursor = decodeWorkItemCursor(condition.nextCursor());
            return findNextWorkItemStartIndex(filtered, cursor);
        }
        if (condition.prevCursor() != null && !condition.prevCursor().isBlank()) {
            UUID cursor = decodeWorkItemCursor(condition.prevCursor());
            return findPreviousWorkItemStartIndex(filtered, cursor, condition.limit());
        }
        return 0;
    }

    private int findNextWorkItemStartIndex(List<ResolvedWorkItem> filtered, UUID cursorRevisionId) {
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).revision().getId().equals(cursorRevisionId)) {
                return i + 1;
            }
        }
        throw new AppException(ErrorCode.VALIDATION_ERROR, "cursor 값이 올바르지 않습니다");
    }

    private int findPreviousWorkItemStartIndex(List<ResolvedWorkItem> filtered, UUID cursorRevisionId, int limit) {
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).revision().getId().equals(cursorRevisionId)) {
                return Math.max(0, i - limit);
            }
        }
        throw new AppException(ErrorCode.VALIDATION_ERROR, "cursor 값이 올바르지 않습니다");
    }

    private String encodeWorkItemCursor(ResolvedWorkItem item) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(item.revision().getId().toString().getBytes(StandardCharsets.UTF_8));
    }

    private UUID decodeWorkItemCursor(String encodedCursor) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(encodedCursor),
                    StandardCharsets.UTF_8
            );
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "cursor 값이 올바르지 않습니다");
        }
    }

    private BomDirection parseBomDirection(String rawDirection) {
        if (rawDirection == null || rawDirection.isBlank()) {
            return BomDirection.FORWARD;
        }
        try {
            return BomDirection.valueOf(rawDirection.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "direction 값이 올바르지 않습니다: " + rawDirection
            );
        }
    }

    private PartLifecycleState parseLifecycleState(String rawLifecycleState) {
        if (rawLifecycleState == null || rawLifecycleState.isBlank()) {
            return null;
        }
        try {
            return PartLifecycleState.valueOf(rawLifecycleState.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "lifecycle_state 값이 올바르지 않습니다: " + rawLifecycleState
            );
        }
    }

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private List<String> findDistinctCategories() {
        return partRevisionRepository.findDistinctCategories();
    }

    private List<PartLifecycleState> findDistinctLifecycleStates() {
        PathBuilder<Part> part = new PathBuilder<>(Part.class, "part");
        var lifecycleStateExpr = part.getEnum("lifecycleState", PartLifecycleState.class);
        return queryFactory()
                .select(lifecycleStateExpr)
                .distinct()
                .from(part)
                .where(lifecycleStateExpr.isNotNull())
                .orderBy(lifecycleStateExpr.asc())
                .fetch();
    }

    private BooleanBuilder buildPartPredicate(
            PathBuilder<Part> part,
            PathBuilder<ProjectPart> projectPart,
            String search,
            String category,
            PartLifecycleState lifecycleState,
            List<UUID> partIds,
            UUID projectId
    ) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (search != null && !search.isBlank()) {
            String keyword = search.trim();
            predicate.and(part.getString("partNumber").containsIgnoreCase(keyword));
        }
        if (lifecycleState != null) {
            predicate.and(part.getEnum("lifecycleState", PartLifecycleState.class).eq(lifecycleState));
        }
        if (partIds != null && !partIds.isEmpty()) {
            predicate.and(part.get("id", UUID.class).in(partIds));
        }
        if (projectId != null) {
            BooleanExpression linkedToProject = JPAExpressions.selectOne()
                    .from(projectPart)
                    .where(
                            projectPart.get("partId", UUID.class).eq(part.get("id", UUID.class))
                                    .and(projectPart.get("projectId", UUID.class).eq(projectId))
                    )
                    .exists();
            predicate.and(linkedToProject);
        }
        return predicate;
    }

    private ResolvedPart resolvePart(Part part) {
        return resolveParts(List.of(part)).stream()
                .findFirst()
                .orElseGet(() -> new ResolvedPart(part, null));
    }

    private List<ResolvedPart> resolveParts(List<Part> parts) {
        if (parts.isEmpty()) {
            return List.of();
        }
        Map<UUID, PartRevision> revisionsByPartId = resolveCurrentRevisions(parts);
        return parts.stream()
                .map(part -> new ResolvedPart(part, revisionsByPartId.get(part.getId())))
                .toList();
    }

    private List<ResolvedPart> resolveReleasedParts(List<Part> parts) {
        if (parts.isEmpty()) {
            return List.of();
        }
        Map<UUID, PartRevision> revisionsByPartId = resolveReleasedRevisions(parts);
        return parts.stream()
                .map(part -> new ResolvedPart(part, revisionsByPartId.get(part.getId())))
                .filter(part -> part.revision() != null)
                .toList();
    }

    private Map<UUID, PartRevision> resolveCurrentRevisions(Iterable<Part> parts) {
        LinkedHashMap<UUID, Part> partsById = new LinkedHashMap<>();
        for (Part part : parts) {
            if (part != null) {
                partsById.put(part.getId(), part);
            }
        }
        if (partsById.isEmpty()) {
            return Map.of();
        }

        List<PartRevision> revisions = partRevisionRepository.findByPartIdInOrderByCreatedAtDesc(partsById.keySet());
        Map<UUID, List<PartRevision>> revisionsByPartId = new LinkedHashMap<>();
        Map<UUID, PartRevision> revisionsById = new HashMap<>();
        for (PartRevision revision : revisions) {
            revisionsByPartId.computeIfAbsent(revision.getPartId(), ignored -> new ArrayList<>()).add(revision);
            revisionsById.put(revision.getId(), revision);
        }

        Map<UUID, PartRevision> resolved = new HashMap<>();
        for (Part part : partsById.values()) {
            resolved.put(part.getId(), resolveCurrentRevision(part, revisionsByPartId, revisionsById));
        }
        return resolved;
    }

    private Map<UUID, PartRevision> resolveReleasedRevisions(Iterable<Part> parts) {
        LinkedHashMap<UUID, Part> partsById = new LinkedHashMap<>();
        for (Part part : parts) {
            if (part != null) {
                partsById.put(part.getId(), part);
            }
        }
        if (partsById.isEmpty()) {
            return Map.of();
        }

        List<PartRevision> revisions = partRevisionRepository.findByPartIdInOrderByCreatedAtDesc(partsById.keySet());
        Map<UUID, List<PartRevision>> revisionsByPartId = new LinkedHashMap<>();
        Map<UUID, PartRevision> revisionsById = new HashMap<>();
        for (PartRevision revision : revisions) {
            revisionsByPartId.computeIfAbsent(revision.getPartId(), ignored -> new ArrayList<>()).add(revision);
            revisionsById.put(revision.getId(), revision);
        }

        Map<UUID, PartRevision> resolved = new HashMap<>();
        for (Part part : partsById.values()) {
            resolved.put(part.getId(), resolveOfficialRevision(part.getCurrentReleasedRevisionId(), revisionsById));
        }
        return resolved;
    }

    private List<ResolvedWorkItem> resolveWorkItems(
            List<Part> parts,
            List<PartRevisionStatus> statuses,
            Map<UUID, PartRevision> baseRevisionsById
    ) {
        if (parts.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<UUID, Part> partsById = new LinkedHashMap<>();
        for (Part part : parts) {
            if (part != null) {
                partsById.put(part.getId(), part);
            }
        }
        if (partsById.isEmpty()) {
            return List.of();
        }

        Set<PartRevisionStatus> statusSet = new LinkedHashSet<>(statuses);
        List<PartRevision> revisions = partRevisionRepository.findByPartIdInOrderByCreatedAtDesc(partsById.keySet()).stream()
                .filter(revision -> statusSet.contains(revision.getStatus()))
                .toList();

        Set<UUID> baseRevisionIds = revisions.stream()
                .map(PartRevision::getBaseRevisionId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!baseRevisionIds.isEmpty()) {
            partRevisionRepository.findAllById(baseRevisionIds)
                    .forEach(baseRevision -> baseRevisionsById.put(baseRevision.getId(), baseRevision));
        }

        return revisions.stream()
                .map(revision -> {
                    Part part = partsById.get(revision.getPartId());
                    if (part == null) {
                        return null;
                    }
                    return new ResolvedWorkItem(part, revision, baseRevisionsById.get(revision.getBaseRevisionId()));
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private PartRevision resolveCurrentRevision(
            Part part,
            Map<UUID, List<PartRevision>> revisionsByPartId,
            Map<UUID, PartRevision> revisionsById
    ) {
        if (part.getCurrentReleasedRevisionId() != null) {
            PartRevision revision = revisionsById.get(part.getCurrentReleasedRevisionId());
            if (revision != null) {
                return revision;
            }
        }
        List<PartRevision> revisions = revisionsByPartId.get(part.getId());
        if (revisions == null || revisions.isEmpty()) {
            return null;
        }
        return revisions.get(0);
    }

    private PartRevision resolveOfficialRevision(UUID revisionId, Map<UUID, PartRevision> revisionsById) {
        if (revisionId == null) {
            return null;
        }
        return revisionsById.get(revisionId);
    }

    private List<PartRevisionStatus> normalizeInProgressStatuses(List<PartInProgressStatusFilter> rawStatuses) {
        if (rawStatuses == null || rawStatuses.isEmpty()) {
            return List.of(PartRevisionStatus.DRAFT);
        }

        LinkedHashSet<PartRevisionStatus> normalized = new LinkedHashSet<>();
        for (PartInProgressStatusFilter status : rawStatuses) {
            if (status == null) {
                continue;
            }
            normalized.add(switch (status) {
                case DRAFT -> PartRevisionStatus.DRAFT;
            });
        }
        if (normalized.isEmpty()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "statuses 값이 올바르지 않습니다"
            );
        }
        return List.copyOf(normalized);
    }

    private List<ResolvedWorkItem> filterResolvedWorkItems(
            List<ResolvedWorkItem> items,
            UUID actorId,
            String search,
            String category,
            boolean mineOnly,
            Boolean hasChildren,
            Boolean hasDrawing
    ) {
        return items.stream()
                .filter(item -> matchesMineOnly(item, actorId, mineOnly))
                .filter(item -> matchesSearch(item, search))
                .filter(item -> matchesCategory(item, category))
                .filter(item -> matchesHasChildren(item, hasChildren))
                .filter(item -> matchesHasDrawing(item, hasDrawing))
                .toList();
    }

    private boolean matchesMineOnly(ResolvedWorkItem item, UUID actorId, boolean mineOnly) {
        if (!mineOnly) {
            return true;
        }
        return actorId.equals(item.revision().getCreatedBy());
    }

    private long countEngineeringBomChildren(PartRevision revision) {
        if (revision == null) {
            return 0L;
        }
        return engineeringBomItemRepository.countByParentPartRevisionId(revision.getId());
    }

    private long countEngineeringBomParents(PartRevision revision) {
        if (revision == null) {
            return 0L;
        }
        return engineeringBomItemRepository.countByChildPartRevisionId(revision.getId());
    }

    private Set<UUID> collectRelatedRevisionIds(
            List<EngineeringBomItem> childItems,
            List<EngineeringBomItem> parentItems,
            UUID rootRevisionId
    ) {
        Set<UUID> revisionIds = new HashSet<>();
        revisionIds.add(rootRevisionId);
        childItems.forEach(item -> revisionIds.add(item.getChildPartRevisionId()));
        parentItems.forEach(item -> revisionIds.add(item.getParentPartRevisionId()));
        return revisionIds;
    }

    private Map<UUID, PartRevision> loadPartRevisions(Set<UUID> revisionIds) {
        if (revisionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, PartRevision> revisionsById = new LinkedHashMap<>();
        partRevisionRepository.findAllById(revisionIds)
                .forEach(revision -> revisionsById.put(revision.getId(), revision));
        return revisionsById;
    }

    private Map<UUID, Part> loadPartsByRevisionIds(Iterable<PartRevision> revisions) {
        Map<UUID, Part> partsById = new LinkedHashMap<>();
        List<UUID> partIds = new ArrayList<>();
        revisions.forEach(revision -> {
            if (revision != null) {
                partIds.add(revision.getPartId());
            }
        });
        partRepository.findAllById(partIds).forEach(part -> partsById.put(part.getId(), part));
        return partsById;
    }

    private List<ResolvedPart> filterResolvedParts(
            List<ResolvedPart> parts,
            String search,
            String category,
            Boolean hasChildren,
            Boolean hasDrawing
    ) {
        return parts.stream()
                .filter(part -> matchesSearch(part, search))
                .filter(part -> matchesCategory(part, category))
                .filter(part -> matchesHasChildren(part, hasChildren))
                .filter(part -> matchesHasDrawing(part, hasDrawing))
                .toList();
    }

    private boolean matchesSearch(ResolvedPart part, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String lowered = search.trim().toLowerCase(java.util.Locale.ROOT);
        return part.partNumber().toLowerCase(java.util.Locale.ROOT).contains(lowered)
                || (part.name() != null && part.name().toLowerCase(java.util.Locale.ROOT).contains(lowered));
    }

    private boolean matchesSearch(ResolvedWorkItem item, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String lowered = search.trim().toLowerCase(java.util.Locale.ROOT);
        return item.part().getPartNumber().toLowerCase(java.util.Locale.ROOT).contains(lowered)
                || (item.revision().getName() != null
                && item.revision().getName().toLowerCase(java.util.Locale.ROOT).contains(lowered));
    }

    private boolean matchesCategory(ResolvedPart part, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        return category.trim().equals(part.category());
    }

    private boolean matchesCategory(ResolvedWorkItem item, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        return category.trim().equals(item.revision().getCategory());
    }

    private boolean matchesHasChildren(ResolvedPart part, Boolean hasChildren) {
        if (hasChildren == null) {
            return true;
        }
        if (part.revision() == null) {
            return !Boolean.TRUE.equals(hasChildren);
        }
        boolean exists = engineeringBomItemRepository.existsByParentPartRevisionId(part.revision().getId());
        return Boolean.TRUE.equals(hasChildren) ? exists : !exists;
    }

    private boolean matchesHasChildren(ResolvedWorkItem item, Boolean hasChildren) {
        if (hasChildren == null) {
            return true;
        }
        boolean exists = engineeringBomItemRepository.existsByParentPartRevisionId(item.revision().getId());
        return Boolean.TRUE.equals(hasChildren) ? exists : !exists;
    }

    private boolean matchesHasDrawing(ResolvedPart part, Boolean hasDrawing) {
        if (hasDrawing == null) {
            return true;
        }
        if (part.revision() == null) {
            return !Boolean.TRUE.equals(hasDrawing);
        }
        boolean exists = drawingRepository.existsByPartRevisionIdAndDeletedAtIsNull(part.revision().getId());
        return Boolean.TRUE.equals(hasDrawing) ? exists : !exists;
    }

    private boolean matchesHasDrawing(ResolvedWorkItem item, Boolean hasDrawing) {
        if (hasDrawing == null) {
            return true;
        }
        boolean exists = drawingRepository.existsByPartRevisionIdAndDeletedAtIsNull(item.revision().getId());
        return Boolean.TRUE.equals(hasDrawing) ? exists : !exists;
    }

    private UUID resolveRequiredPartId(String partNumber, String revisionCode) {
        return resolveRequiredPart(partNumber, revisionCode).id();
    }

    private boolean isOfficialRevision(PartRevision revision) {
        return revision.getRevisionCode() != null && !revision.getRevisionCode().isBlank();
    }

    private Map<UUID, List<File>> groupFilesByRevisionId(List<PartRevision> revisions) {
        List<UUID> revisionIds = revisions.stream().map(PartRevision::getId).toList();
        return fileRepository.findByOwnerTypeAndOwnerIdInAndStatusAndDeletedAtIsNull(
                        "part_revision",
                        revisionIds,
                        FileStatus.UPLOADED
                ).stream()
                .collect(java.util.stream.Collectors.groupingBy(File::getOwnerId));
    }

    private Map<UUID, List<Drawing>> groupDrawingsByRevisionId(List<PartRevision> revisions) {
        List<UUID> revisionIds = revisions.stream().map(PartRevision::getId).toList();
        return drawingRepository.findByPartRevisionIdInAndDeletedAtIsNullOrderByCreatedAtDesc(revisionIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(Drawing::getPartRevisionId));
    }

    private Map<UUID, File> loadDrawingSourceFiles(Map<UUID, List<Drawing>> drawingsByRevisionId) {
        Set<UUID> sourceFileIds = drawingsByRevisionId.values().stream()
                .flatMap(List::stream)
                .map(Drawing::getSourceFileId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (sourceFileIds.isEmpty()) {
            return Map.of();
        }
        return fileRepository.findByIdIn(sourceFileIds).stream()
                .collect(java.util.stream.Collectors.toMap(File::getId, file -> file));
    }

    private Map<UUID, List<EngineeringBomItem>> groupBomItemsByRevisionId(List<PartRevision> revisions) {
        List<UUID> revisionIds = revisions.stream().map(PartRevision::getId).toList();
        return engineeringBomItemRepository.findByParentPartRevisionIdInOrderByParentPartRevisionIdAscCreatedAtAsc(revisionIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(EngineeringBomItem::getParentPartRevisionId));
    }

    private Map<UUID, List<PartRevisionHistory>> groupHistoriesByRevisionId(List<PartRevision> revisions) {
        List<UUID> revisionIds = revisions.stream().map(PartRevision::getId).toList();
        return partRevisionHistoryRepository.findByPartRevisionIdInOrderByOccurredAtAsc(revisionIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(PartRevisionHistory::getPartRevisionId));
    }

    private Map<UUID, User> loadUsersByRevisionHistory(
            List<PartRevision> revisions,
            Map<UUID, List<PartRevisionHistory>> historiesByRevisionId
    ) {
        LinkedHashSet<UUID> userIds = new LinkedHashSet<>();
        revisions.stream()
                .map(PartRevision::getCreatedBy)
                .filter(java.util.Objects::nonNull)
                .forEach(userIds::add);
        historiesByRevisionId.values().stream()
                .flatMap(List::stream)
                .map(PartRevisionHistory::getActorId)
                .filter(java.util.Objects::nonNull)
                .forEach(userIds::add);
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userApi.getUsersByIdsOrdered(List.copyOf(userIds)).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));
    }

    private String extractReason(String payload) {
        if (payload == null || payload.isBlank() || "{}".equals(payload.trim())) {
            return null;
        }
        Matcher matcher = HISTORY_REASON_PATTERN.matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private String extractHistoryIdentifier(String payload) {
        if (payload == null || payload.isBlank() || "{}".equals(payload.trim())) {
            return null;
        }
        Matcher matcher = HISTORY_IDENTIFIER_PATTERN.matcher(payload);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private Instant resolveReleaseOccurredAt(
            PartRevision revision,
            Map<UUID, List<PartRevisionHistory>> historiesByRevisionId
    ) {
        PartRevisionHistory history = findHistoryByAction(
                historiesByRevisionId.getOrDefault(revision.getId(), List.of()),
                PartRevisionHistoryActionType.RELEASED
        );
        return history == null ? revision.getCreatedAt() : history.getOccurredAt();
    }

    private PartRevisionHistory findHistoryByAction(List<PartRevisionHistory> histories, PartRevisionHistoryActionType actionType) {
        return histories.stream()
                .filter(history -> history.getActionType() == actionType)
                .findFirst()
                .orElse(null);
    }

    private PartRevisionHistoryResult.Draft toHistoryDraft(
            PartRevision revision,
            List<PartRevisionHistory> histories,
            Map<UUID, User> usersById
    ) {
        PartRevisionHistory completionHistory = switch (revision.getStatus()) {
            case CANCELED -> findHistoryByAction(histories, PartRevisionHistoryActionType.CANCELED);
            case RELEASED, SUPERSEDED -> findHistoryByAction(histories, PartRevisionHistoryActionType.RELEASED);
            default -> null;
        };
        String releasedRevisionCode = switch (revision.getStatus()) {
            case RELEASED, SUPERSEDED -> revision.getRevisionCode();
            default -> null;
        };

        return new PartRevisionHistoryResult.Draft(
                revision.getId(),
                revision.getName(),
                revision.getStatus(),
                revision.getCreatedAt(),
                toUserSummary(usersById.get(revision.getCreatedBy())),
                completionHistory == null ? null : completionHistory.getOccurredAt(),
                completionHistory == null ? null : toUserSummary(usersById.get(completionHistory.getActorId())),
                releasedRevisionCode,
                completionHistory == null ? null : extractReason(completionHistory.getPayload())
        );
    }

    private PartRevision resolveBaseRevision(PartRevision targetRevision, String baseRevisionCode) {
        if (baseRevisionCode != null && !baseRevisionCode.isBlank()) {
            return partRevisionRepository.findByPartNumberAndRevisionCode(targetRevision.getPartNumber(), baseRevisionCode)
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "PartRevision '%s/%s'을(를) 찾을 수 없습니다"
                                    .formatted(targetRevision.getPartNumber(), baseRevisionCode)
                    ));
        }

        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(targetRevision.getPartId()).stream()
                .filter(this::isOfficialRevision)
                .toList();
        for (int index = 0; index < revisions.size(); index++) {
            if (revisions.get(index).getId().equals(targetRevision.getId())) {
                return index + 1 < revisions.size() ? revisions.get(index + 1) : null;
            }
        }
        return null;
    }

    private RevisionDiffSnapshot buildRevisionDiffSnapshot(
            PartRevision baseRevision,
            PartRevision targetRevision,
            Map<UUID, List<File>> filesByRevisionId,
            Map<UUID, List<Drawing>> drawingsByRevisionId,
            Map<UUID, File> drawingSourceFilesById,
            Map<UUID, List<EngineeringBomItem>> bomItemsByRevisionId,
            Map<UUID, User> usersById
    ) {
        List<PartRevisionDiffResult.AttributeChange> attributeChanges = compareAttributeChanges(baseRevision, targetRevision);
        List<PartRevisionDiffResult.FileChange> fileChanges = compareFileChanges(
                baseRevision,
                targetRevision,
                filesByRevisionId,
                drawingsByRevisionId,
                drawingSourceFilesById
        );
        List<PartRevisionDiffResult.BomChange> bomChanges = compareBomChanges(baseRevision, targetRevision, bomItemsByRevisionId);

        return new RevisionDiffSnapshot(
                new PartRevisionDiffSummaryResult(
                        attributeChanges.size(),
                        fileChanges.size(),
                        bomChanges.size()
                ),
                attributeChanges,
                fileChanges,
                bomChanges
        );
    }

    private List<PartRevisionDiffResult.AttributeChange> compareAttributeChanges(
            PartRevision baseRevision,
            PartRevision targetRevision
    ) {
        List<PartRevisionDiffResult.AttributeChange> changes = new ArrayList<>();
        addAttributeChange(changes, "name", "품명", baseRevision.getName(), targetRevision.getName());
        addAttributeChange(changes, "material", "재질", baseRevision.getMaterial(), targetRevision.getMaterial());
        addAttributeChange(changes, "unit", "단위", baseRevision.getUnit(), targetRevision.getUnit());
        addAttributeChange(changes, "description", "설명", baseRevision.getDescription(), targetRevision.getDescription());
        addAttributeChange(changes, "category", "카테고리", baseRevision.getCategory(), targetRevision.getCategory());
        addAttributeChange(changes, "phantom", "팬텀", formatScalar(baseRevision.getPhantom()), formatScalar(targetRevision.getPhantom()));
        addAttributeChange(
                changes,
                "leadTimeDays",
                "리드타임",
                formatScalar(baseRevision.getLeadTimeDays()),
                formatScalar(targetRevision.getLeadTimeDays())
        );

        Map<String, Object> beforeProperties = parseExtendedProperties(baseRevision.getExtendedProperties());
        Map<String, Object> afterProperties = parseExtendedProperties(targetRevision.getExtendedProperties());
        TreeSet<String> propertyKeys = new TreeSet<>();
        propertyKeys.addAll(beforeProperties.keySet());
        propertyKeys.addAll(afterProperties.keySet());
        for (String propertyKey : propertyKeys) {
            addAttributeChange(
                    changes,
                    "extendedProperties." + propertyKey,
                    propertyKey,
                    formatScalar(beforeProperties.get(propertyKey)),
                    formatScalar(afterProperties.get(propertyKey))
            );
        }
        return changes;
    }

    private void addAttributeChange(
            List<PartRevisionDiffResult.AttributeChange> changes,
            String fieldKey,
            String fieldLabel,
            String beforeValue,
            String afterValue
    ) {
        if (java.util.Objects.equals(beforeValue, afterValue)) {
            return;
        }
        changes.add(new PartRevisionDiffResult.AttributeChange(
                fieldKey,
                fieldLabel,
                resolveChangeType(beforeValue, afterValue),
                beforeValue,
                afterValue
        ));
    }

    private List<PartRevisionDiffResult.FileChange> compareFileChanges(
            PartRevision baseRevision,
            PartRevision targetRevision,
            Map<UUID, List<File>> filesByRevisionId,
            Map<UUID, List<Drawing>> drawingsByRevisionId,
            Map<UUID, File> drawingSourceFilesById
    ) {
        Map<String, FileDiffEntry> beforeEntries = indexFileDiffEntries(
                baseRevision.getId(),
                filesByRevisionId,
                drawingsByRevisionId,
                drawingSourceFilesById
        );
        Map<String, FileDiffEntry> afterEntries = indexFileDiffEntries(
                targetRevision.getId(),
                filesByRevisionId,
                drawingsByRevisionId,
                drawingSourceFilesById
        );

        TreeSet<String> keys = new TreeSet<>();
        keys.addAll(beforeEntries.keySet());
        keys.addAll(afterEntries.keySet());

        List<PartRevisionDiffResult.FileChange> changes = new ArrayList<>();
        for (String key : keys) {
            FileDiffEntry before = beforeEntries.get(key);
            FileDiffEntry after = afterEntries.get(key);
            if (before != null && after != null) {
                continue;
            }
            FileDiffEntry entry = after != null ? after : before;
            changes.add(new PartRevisionDiffResult.FileChange(
                    entry.itemType(),
                    entry.displayName(),
                    after != null ? PartRevisionDiffChangeType.ADDED : PartRevisionDiffChangeType.REMOVED
            ));
        }
        return changes;
    }

    private Map<String, FileDiffEntry> indexFileDiffEntries(
            UUID revisionId,
            Map<UUID, List<File>> filesByRevisionId,
            Map<UUID, List<Drawing>> drawingsByRevisionId,
            Map<UUID, File> drawingSourceFilesById
    ) {
        LinkedHashMap<String, FileDiffEntry> entries = new LinkedHashMap<>();
        for (File file : filesByRevisionId.getOrDefault(revisionId, List.of())) {
            entries.putIfAbsent(
                    "FILE:" + file.getOriginalName(),
                    new FileDiffEntry("FILE", file.getOriginalName())
            );
        }
        for (Drawing drawing : drawingsByRevisionId.getOrDefault(revisionId, List.of())) {
            File sourceFile = drawingSourceFilesById.get(drawing.getSourceFileId());
            String displayName = sourceFile != null
                    ? sourceFile.getOriginalName()
                    : drawing.getDrawingNumber() != null
                            ? drawing.getDrawingNumber()
                            : drawing.getName();
            entries.putIfAbsent(
                    "DRAWING:" + displayName,
                    new FileDiffEntry("DRAWING", displayName)
            );
        }
        return entries;
    }

    private List<PartRevisionDiffResult.BomChange> compareBomChanges(
            PartRevision baseRevision,
            PartRevision targetRevision,
            Map<UUID, List<EngineeringBomItem>> bomItemsByRevisionId
    ) {
        Map<String, EngineeringBomItem> beforeItems = bomItemsByRevisionId.getOrDefault(baseRevision.getId(), List.of()).stream()
                .collect(java.util.stream.Collectors.toMap(EngineeringBomItem::getLineNumber, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, EngineeringBomItem> afterItems = bomItemsByRevisionId.getOrDefault(targetRevision.getId(), List.of()).stream()
                .collect(java.util.stream.Collectors.toMap(EngineeringBomItem::getLineNumber, item -> item, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> childRevisionIds = new LinkedHashSet<>();
        beforeItems.values().forEach(item -> childRevisionIds.add(item.getChildPartRevisionId()));
        afterItems.values().forEach(item -> childRevisionIds.add(item.getChildPartRevisionId()));
        Map<UUID, PartRevision> childRevisionsById = loadPartRevisions(childRevisionIds);
        Map<UUID, Part> childPartsById = loadPartsByRevisionIds(childRevisionsById.values());

        TreeSet<String> lineNumbers = new TreeSet<>(this::compareLineNumbers);
        lineNumbers.addAll(beforeItems.keySet());
        lineNumbers.addAll(afterItems.keySet());

        List<PartRevisionDiffResult.BomChange> changes = new ArrayList<>();
        for (String lineNumber : lineNumbers) {
            EngineeringBomItem before = beforeItems.get(lineNumber);
            EngineeringBomItem after = afterItems.get(lineNumber);
            if (before != null && after != null
                    && java.util.Objects.equals(before.getChildPartRevisionId(), after.getChildPartRevisionId())
                    && java.util.Objects.equals(before.getQuantity(), after.getQuantity())
                    && java.util.Objects.equals(before.getExtendedProperties(), after.getExtendedProperties())) {
                continue;
            }

            PartRevision beforeRevision = before == null ? null : childRevisionsById.get(before.getChildPartRevisionId());
            PartRevision afterRevision = after == null ? null : childRevisionsById.get(after.getChildPartRevisionId());
            Part beforePart = beforeRevision == null ? null : childPartsById.get(beforeRevision.getPartId());
            Part afterPart = afterRevision == null ? null : childPartsById.get(afterRevision.getPartId());

            changes.add(new PartRevisionDiffResult.BomChange(
                    lineNumber,
                    beforePart == null ? null : beforePart.getPartNumber(),
                    beforeRevision == null ? null : beforeRevision.getName(),
                    before == null ? null : before.getQuantity(),
                    afterPart == null ? null : afterPart.getPartNumber(),
                    afterRevision == null ? null : afterRevision.getName(),
                    after == null ? null : after.getQuantity(),
                    resolveChangeType(before, after)
            ));
        }
        return changes;
    }

    private String formatScalar(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private PartRevisionDiffChangeType resolveChangeType(Object beforeValue, Object afterValue) {
        if (beforeValue == null && afterValue != null) {
            return PartRevisionDiffChangeType.ADDED;
        }
        if (beforeValue != null && afterValue == null) {
            return PartRevisionDiffChangeType.REMOVED;
        }
        return PartRevisionDiffChangeType.CHANGED;
    }

    private ResolvedPart resolveRequiredDraft(String partNumber, String baseRevisionCode, String draftKey) {
        PartRevision draft = (baseRevisionCode == null || baseRevisionCode.isBlank()
                ? partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionIdIsNull(partNumber, draftKey)
                : findRevisionScopedDraft(partNumber, baseRevisionCode, draftKey))
                .filter(revision -> revision.getStatus() == com.fabbitinc.server.domain.part.model.PartRevisionStatus.DRAFT)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartDraft '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, draftKey)
                ));
        Part part = partRepository.findById(draft.getPartId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partNumber)
                ));
        return new ResolvedPart(part, draft);
    }

    private ResolvedPart resolveRequiredTarget(
            String partNumber,
            String revisionCode,
            String baseRevisionCode,
            String draftKey
    ) {
        if (draftKey != null && !draftKey.isBlank()) {
            return resolveRequiredDraft(partNumber, baseRevisionCode, draftKey);
        }
        return resolveRequiredPart(partNumber, revisionCode);
    }

    private java.util.Optional<PartRevision> findRevisionScopedDraft(String partNumber, String baseRevisionCode, String draftKey) {
        PartRevision baseRevision = partRevisionRepository.findByPartNumberAndRevisionCode(partNumber, baseRevisionCode)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, baseRevisionCode)
                ));
        return partRevisionRepository.findByPartNumberAndDraftKeyAndBaseRevisionId(
                partNumber,
                draftKey,
                baseRevision.getId()
        );
    }

    private ResolvedPart resolveRequiredPart(String partNumber, String revisionCode) {
        PartRevision revision = partRevisionRepository.findByPartNumberAndRevisionCode(partNumber, revisionCode)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s/%s'을(를) 찾을 수 없습니다".formatted(partNumber, revisionCode)
                ));
        Part part = partRepository.findById(revision.getPartId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '%s'을(를) 찾을 수 없습니다".formatted(partNumber)
                ));
        return new ResolvedPart(part, revision);
    }

    private String resolveName(PartRevision revision) {
        return revision == null ? null : revision.getName();
    }

    private String resolveRevisionCode(PartRevision revision) {
        if (revision == null || revision.getRevisionCode() == null || revision.getRevisionCode().isBlank()) {
            return null;
        }
        return revision.getRevisionCode();
    }

    private void flattenBomTree(BomTreeResult.Node node, int level, List<BomFlatRow> rows) {
        rows.add(new BomFlatRow(
                level,
                node.partNumber(),
                node.name(),
                node.revision(),
                node.quantity(),
                node.material(),
                node.unit(),
                node.category(),
                node.lifecycleState()
        ));
        for (BomTreeResult.Node child : node.children()) {
            flattenBomTree(child, level + 1, rows);
        }
    }

    private BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return BigDecimal.ONE;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        return new BigDecimal(value.toString()).stripTrailingZeros();
    }

    private int compareLineNumbers(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        Integer leftNumber = parseLineNumber(left);
        Integer rightNumber = parseLineNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Integer.compare(leftNumber, rightNumber);
        }
        return left.compareTo(right);
    }

    private Integer parseLineNumber(String lineNumber) {
        if (lineNumber == null || lineNumber.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(lineNumber.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void writeHeader(Sheet sheet, List<String> headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            writeCell(header, i, headers.get(i));
        }
    }

    private void writeCell(Row row, int colIndex, Object value) {
        if (value == null) {
            return;
        }
        row.createCell(colIndex).setCellValue(String.valueOf(value));
    }

    private void autoFitColumns(Sheet sheet, int size) {
        for (int i = 0; i < size; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            int maxWidth = 50 * 256;
            if (width > maxWidth) {
                sheet.setColumnWidth(i, maxWidth);
            }
        }
    }

    private Map<String, Object> parseExtendedProperties(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }

        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return Map.of();
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isBlank()) {
            return Map.of();
        }

        Map<String, Object> parsed = new LinkedHashMap<>();
        for (String chunk : splitTopLevel(body)) {
            int delimiterIdx = findDelimiter(chunk);
            if (delimiterIdx < 0) {
                continue;
            }
            String rawKey = chunk.substring(0, delimiterIdx).trim();
            String rawValue = chunk.substring(delimiterIdx + 1).trim();

            Matcher keyMatcher = STRING_PATTERN.matcher(rawKey);
            if (!keyMatcher.find()) {
                continue;
            }
            String key = unescapeJsonString(keyMatcher.group(1));
            Object value = parseLiteralValue(rawValue);
            if (value != null) {
                parsed.put(key, value);
            }
        }

        return parsed;
    }

    private List<String> splitTopLevel(String body) {
        List<String> chunks = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                current.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '"') {
                current.append(ch);
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ',' && !inQuotes) {
                chunks.add(current.toString());
                current = new StringBuilder();
                continue;
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private int findDelimiter(String chunk) {
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < chunk.length(); i++) {
            char ch = chunk.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ':' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    private Object parseLiteralValue(String rawValue) {
        if ("null".equals(rawValue)) {
            return null;
        }
        if ("true".equals(rawValue) || "false".equals(rawValue)) {
            return Boolean.valueOf(rawValue);
        }

        Matcher stringMatcher = STRING_PATTERN.matcher(rawValue);
        if (stringMatcher.find()) {
            return unescapeJsonString(stringMatcher.group(1));
        }

        if (NUMBER_PATTERN.matcher(rawValue).matches()) {
            if (rawValue.contains(".")) {
                try {
                    return Double.valueOf(rawValue);
                } catch (NumberFormatException ignored) {
                    return rawValue;
                }
            }
            try {
                return Long.valueOf(rawValue);
            } catch (NumberFormatException ignored) {
                return rawValue;
            }
        }

        return rawValue;
    }

    private String unescapeJsonString(String raw) {
        return raw
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private void assertPartExists(UUID partId) {
        if (partRepository.findById(partId).isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "Part '" + partId + "'을(를) 찾을 수 없습니다");
        }
    }

    private record BomFlatRow(
            int level,
            String partNumber,
            String name,
            String revision,
            BigDecimal quantity,
            String material,
            String unit,
            String category,
            PartLifecycleState lifecycleState
    ) {
    }

    private record ActivePreviewSource(
            PartPreviewSourceType sourceType,
            UUID sourceId
    ) {
    }

    private record ResolvedPart(
            Part part,
            PartRevision revision
    ) {
        UUID id() {
            return part.getId();
        }

        String partNumber() {
            return part.getPartNumber();
        }

        String name() {
            return revision == null ? null : revision.getName();
        }

        String revisionCode() {
            return revision == null ? null : revision.getRevisionCode();
        }

        String draftKey() {
            return revision == null ? null : revision.getDraftKey();
        }

        String material() {
            return revision == null ? null : revision.getMaterial();
        }

        String unit() {
            return revision == null ? null : revision.getUnit();
        }

        String description() {
            return revision == null ? null : revision.getDescription();
        }

        String category() {
            return revision == null ? null : revision.getCategory();
        }

        Boolean phantom() {
            return revision == null ? null : revision.getPhantom();
        }

        Integer leadTimeDays() {
            return revision == null ? null : revision.getLeadTimeDays();
        }

        String extendedProperties() {
            return revision == null ? "{}" : revision.getExtendedProperties();
        }

        PartLifecycleState lifecycleState() {
            return part.getLifecycleState();
        }
    }

    private record ResolvedWorkItem(
            Part part,
            PartRevision revision,
            PartRevision baseRevision
    ) {
        String baseRevisionCode() {
            return baseRevision == null ? null : baseRevision.getRevisionCode();
        }
    }

    private record PartListCursor(
            String partNumber,
            UUID partId
    ) {
    }

    private RevisionWorkflowCounts countRevisionWorkflows(UUID partId) {
        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId);
        long draftCount = revisions.stream()
                .filter(revision -> revision.getStatus() == PartRevisionStatus.DRAFT)
                .count();
        return new RevisionWorkflowCounts(draftCount);
    }

    private record RevisionWorkflowCounts(
            long draftCount
    ) {
    }

    private record RevisionDiffSnapshot(
            PartRevisionDiffSummaryResult summary,
            List<PartRevisionDiffResult.AttributeChange> attributes,
            List<PartRevisionDiffResult.FileChange> files,
            List<PartRevisionDiffResult.BomChange> bom
    ) {
    }

    private record FileDiffEntry(
            String itemType,
            String displayName
    ) {
    }

    private record BomEdge(
            UUID parentRevisionId,
            UUID childRevisionId,
            String lineNumber,
            BigDecimal quantity
    ) {
    }
}
