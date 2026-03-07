package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.part.model.BomDirection;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartLookupCondition;
import com.fabbitinc.server.application.part.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.part.query.condition.PartSuppliersCondition;
import com.fabbitinc.server.application.part.query.result.BomTreeResult;
import com.fabbitinc.server.application.part.query.result.CategoryLookupResult;
import com.fabbitinc.server.application.part.query.result.CategoryStatsResult;
import com.fabbitinc.server.application.part.query.result.PartBomResult;
import com.fabbitinc.server.application.part.query.result.PartDetailResult;
import com.fabbitinc.server.application.part.query.result.PartFilesResult;
import com.fabbitinc.server.application.part.query.result.PartFilterOptionsResult;
import com.fabbitinc.server.application.part.query.result.PartListResult;
import com.fabbitinc.server.application.part.query.result.PartLookupResult;
import com.fabbitinc.server.application.part.query.result.PartProjectsResult;
import com.fabbitinc.server.application.part.query.result.PartSuppliersResult;
import com.fabbitinc.server.application.part.query.result.PartUserSummaryResult;
import com.fabbitinc.server.application.part.query.result.RelatedDrawingResult;
import com.fabbitinc.server.application.project.api.ProjectApi;
import com.fabbitinc.server.application.team.api.TeamApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.BomLink;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartSupplier;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import com.fabbitinc.server.domain.team.model.Team;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
    private final PartRepository partRepository;
    private final FileRepository fileRepository;
    private final BomLinkRepository bomLinkRepository;
    private final PartSupplierRepository partSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final DrawingRepository drawingRepository;
    private final ProjectApi projectApi;
    private final UserApi userApi;
    private final TeamApi teamApi;
    private final FileUrlResolver fileUrlResolver;
    private final EntityManager entityManager;

    private static final Pattern STRING_PATTERN = Pattern.compile("^\"(.*)\"$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");
    private static final int MAX_BOM_DEPTH = 30;

    public PartLookupResult lookup(PartLookupCondition condition) {
        currentAuthProvider.getCurrentAuth();

        List<Part> parts;
        if (condition.search() == null || condition.search().isBlank()) {
            parts = partRepository.findAllByOrderByPartNumberAsc(PageRequest.of(0, condition.limit()));
        } else {
            String normalizedSearch = condition.search().trim();
            parts = partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                    normalizedSearch,
                    normalizedSearch,
                    PageRequest.of(0, condition.limit())
            );
        }

        List<PartLookupResult.Item> items = parts.stream()
                .map(part -> new PartLookupResult.Item(part.getId(), part.getPartNumber(), part.getName()))
                .toList();
        return new PartLookupResult(items);
    }

    public CategoryStatsResult listCategories() {
        currentAuthProvider.getCurrentAuth();

        PathBuilder<Part> part = new PathBuilder<>(Part.class, "part");
        var categoryExpr = part.getString("category");
        var partIdExpr = part.get("id", UUID.class);
        var countExpr = partIdExpr.count();

        List<CategoryStatsResult.Item> items = queryFactory()
                .select(categoryExpr, countExpr)
                .from(part)
                .where(categoryExpr.isNotNull())
                .groupBy(categoryExpr)
                .orderBy(categoryExpr.asc())
                .fetch().stream()
                .map(row -> new CategoryStatsResult.Item(
                        row.get(categoryExpr),
                        row.get(countExpr) == null ? 0L : row.get(countExpr)
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

        PathBuilder<Part> part = new PathBuilder<>(Part.class, "part");
        PathBuilder<Drawing> drawing = new PathBuilder<>(Drawing.class, "drawing");
        PathBuilder<BomLink> bomLink = new PathBuilder<>(BomLink.class, "bomLink");
        PathBuilder<ProjectPart> projectPart = new PathBuilder<>(ProjectPart.class, "projectPart");

        var partIdExpr = part.get("id", UUID.class);
        var partNumberExpr = part.getString("partNumber");
        var partNameExpr = part.getString("name");
        var partCategoryExpr = part.getString("category");
        var partRevisionExpr = part.getString("revision");
        var partLifecycleStateExpr = part.getString("lifecycleState");
        var drawingIdExpr = part.get("drawingId", UUID.class);

        BooleanBuilder predicate = buildPartPredicate(
                part,
                bomLink,
                projectPart,
                condition.search(),
                condition.category(),
                lifecycleState,
                condition.hasDrawing(),
                condition.hasChildren(),
                null,
                condition.projectId()
        );

        Long totalCount = queryFactory()
                .select(partIdExpr.count())
                .from(part)
                .where(predicate)
                .fetchOne();
        long total = totalCount == null ? 0L : totalCount;

        var drawingNumberExpr = JPAExpressions.select(drawing.getString("drawingNumber"))
                .from(drawing)
                .where(drawing.get("id", UUID.class).eq(drawingIdExpr));
        var childrenCountExpr = JPAExpressions.select(bomLink.get("id", UUID.class).count())
                .from(bomLink)
                .where(bomLink.get("parentPartId", UUID.class).eq(partIdExpr));

        List<Tuple> rows = queryFactory()
                .select(
                        partIdExpr,
                        partNumberExpr,
                        partNameExpr,
                        partCategoryExpr,
                        partRevisionExpr,
                        partLifecycleStateExpr,
                        drawingNumberExpr,
                        childrenCountExpr
                )
                .from(part)
                .where(predicate)
                .orderBy(partNumberExpr.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        List<PartListResult.Item> items = rows.stream()
                .map(row -> new PartListResult.Item(
                        row.get(partIdExpr),
                        row.get(partNumberExpr),
                        row.get(partNameExpr),
                        row.get(partCategoryExpr),
                        row.get(partRevisionExpr) == null ? "1" : row.get(partRevisionExpr),
                        PartLifecycleState.from(row.get(partLifecycleStateExpr)),
                        row.get(drawingNumberExpr),
                        row.get(childrenCountExpr) == null ? 0L : row.get(childrenCountExpr)
                ))
                .toList();
        return new PartListResult(total, condition.offset(), condition.limit(), items);
    }

    public byte[] export(PartExportCondition condition) {
        currentAuthProvider.getCurrentAuth();
        PartLifecycleState lifecycleState = parseLifecycleState(condition.lifecycleState());
        List<Part> parts = findPartsForExport(
                condition.search(),
                condition.category(),
                lifecycleState,
                condition.hasDrawing(),
                condition.hasChildren(),
                condition.partIds(),
                condition.projectId()
        );

        Set<String> extKeys = new TreeSet<>();
        Map<UUID, Map<String, Object>> extValues = new HashMap<>();
        for (Part part : parts) {
            Map<String, Object> parsed = parseExtendedProperties(part.getExtendedProperties());
            extValues.put(part.getId(), parsed);
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

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("부품목록");
            writeHeader(sheet, columns);

            int rowIndex = 1;
            for (Part part : parts) {
                Row row = sheet.createRow(rowIndex++);
                Map<String, Object> extended = extValues.getOrDefault(part.getId(), Map.of());

                writeCell(row, 0, part.getPartNumber());
                writeCell(row, 1, part.getName());
                writeCell(row, 2, part.getRevision());
                writeCell(row, 3, part.getMaterial());
                writeCell(row, 4, part.getUnit());
                writeCell(row, 5, part.getDescription());
                writeCell(row, 6, part.getCategory());
                writeCell(row, 7, part.getPhantom());
                writeCell(row, 8, part.getLifecycleState());
                writeCell(row, 9, part.getLeadTimeDays());

                int colIndex = 10;
                for (String key : extKeys) {
                    writeCell(row, colIndex++, extended.get(key));
                }
            }
            autoFitColumns(sheet, columns.size());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "엑셀 파일 생성에 실패했습니다");
        }
    }

    public byte[] exportBomTree(BomTreeExportCondition condition) {
        BomTreeResult tree = getBomTree(new BomTreeCondition(condition.partId(), condition.direction()));
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

        Part part = partRepository.findById(condition.partId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + condition.partId() + "'을(를) 찾을 수 없습니다"
                ));

        PartUserSummaryResult owner = toUserSummary(userApi.getUserOrNull(part.getOwnerId()));
        Team ownerTeam = teamApi.getTeamOrNull(part.getOwnerTeamId());
        String ownerTeamName = ownerTeam == null ? null : ownerTeam.getName();
        RelatedDrawingResult drawing = loadDrawing(part.getDrawingId());

        long childrenCount = bomLinkRepository.countByParentPartId(part.getId());
        long parentsCount = bomLinkRepository.countByChildPartId(part.getId());
        long suppliersCount = partSupplierRepository.countByPartId(part.getId());
        long filesCount = fileRepository.countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                "part",
                part.getId(),
                FileStatus.UPLOADED
        );
        long projectsCount = projectApi.countPartProjects(part.getId());

        return new PartDetailResult(
                part.getId(),
                part.getPartNumber(),
                part.getName(),
                part.getRevision() == null ? "1" : part.getRevision(),
                part.getMaterial(),
                part.getUnit(),
                part.getDescription(),
                part.getCategory(),
                part.getLifecycleState(),
                part.getPhantom(),
                part.getLeadTimeDays(),
                parseExtendedProperties(part.getExtendedProperties()),
                part.getOwnerId(),
                owner,
                part.getOwnerTeamId(),
                ownerTeamName,
                drawing,
                childrenCount,
                parentsCount,
                suppliersCount,
                filesCount,
                projectsCount
        );
    }

    public PartProjectsResult get(PartProjectsCondition condition) {
        currentAuthProvider.getCurrentAuth();
        var result = projectApi.listPartProjects(condition.partId());
        return new PartProjectsResult(
                result.total(),
                result.items().stream()
                        .map(item -> new PartProjectsResult.Item(item.id(), item.name(), item.description()))
                        .toList()
        );
    }

    public PartBomResult get(PartBomCondition condition) {
        currentAuthProvider.getCurrentAuth();
        assertPartExists(condition.partId());

        PathBuilder<BomLink> bomLink = new PathBuilder<>(BomLink.class, "bomLink");
        PathBuilder<Part> childPart = new PathBuilder<>(Part.class, "childPart");
        PathBuilder<Part> parentPart = new PathBuilder<>(Part.class, "parentPart");
        var quantityExpr = bomLink.getNumber("quantity", Integer.class);
        var extendedPropertiesExpr = bomLink.getString("extendedProperties");
        var childIdExpr = childPart.get("id", UUID.class);
        var childPartNumberExpr = childPart.getString("partNumber");
        var childNameExpr = childPart.getString("name");
        var parentIdExpr = parentPart.get("id", UUID.class);
        var parentPartNumberExpr = parentPart.getString("partNumber");
        var parentNameExpr = parentPart.getString("name");

        List<Tuple> childRows = queryFactory()
                .select(
                        childIdExpr,
                        childPartNumberExpr,
                        childNameExpr,
                        quantityExpr,
                        extendedPropertiesExpr
                )
                .from(bomLink)
                .join(childPart).on(childIdExpr.eq(bomLink.get("childPartId", UUID.class)))
                .where(bomLink.get("parentPartId", UUID.class).eq(condition.partId()))
                .orderBy(childPartNumberExpr.asc())
                .fetch();

        List<Tuple> parentRows = queryFactory()
                .select(
                        parentIdExpr,
                        parentPartNumberExpr,
                        parentNameExpr,
                        quantityExpr,
                        extendedPropertiesExpr
                )
                .from(bomLink)
                .join(parentPart).on(parentIdExpr.eq(bomLink.get("parentPartId", UUID.class)))
                .where(bomLink.get("childPartId", UUID.class).eq(condition.partId()))
                .orderBy(parentPartNumberExpr.asc())
                .fetch();

        List<PartBomResult.Child> children = childRows.stream()
                .map(row -> new PartBomResult.Child(
                        row.get(childIdExpr),
                        row.get(childPartNumberExpr),
                        row.get(childNameExpr),
                        row.get(quantityExpr) == null
                                ? 1
                                : row.get(quantityExpr),
                        parseExtendedProperties(row.get(extendedPropertiesExpr))
                ))
                .toList();

        List<PartBomResult.Parent> parents = parentRows.stream()
                .map(row -> new PartBomResult.Parent(
                        row.get(parentIdExpr),
                        row.get(parentPartNumberExpr),
                        row.get(parentNameExpr),
                        row.get(quantityExpr) == null
                                ? 1
                                : row.get(quantityExpr),
                        parseExtendedProperties(row.get(extendedPropertiesExpr))
                ))
                .toList();

        return new PartBomResult(children, parents);
    }

    public BomTreeResult getBomTree(BomTreeCondition condition) {
        currentAuthProvider.getCurrentAuth();

        Part rootPart = partRepository.findById(condition.partId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + condition.partId() + "'을(를) 찾을 수 없습니다"
                ));

        BomDirection resolvedDirection = parseBomDirection(condition.direction());
        boolean reverse = resolvedDirection == BomDirection.REVERSE;
        List<BomEdge> edges = fetchBomEdges(rootPart.getId(), reverse);

        Set<String> allPartNumbers = new HashSet<>();
        allPartNumbers.add(rootPart.getPartNumber());
        for (BomEdge edge : edges) {
            allPartNumbers.add(edge.parentPn());
            allPartNumbers.add(edge.childPn());
        }

        Map<String, Part> partsMap = partRepository.findByPartNumberIn(allPartNumbers).stream()
                .collect(java.util.stream.Collectors.toMap(Part::getPartNumber, part -> part));
        BomTreeResult.Node root = buildBomTree(rootPart.getPartNumber(), edges, partsMap);

        return new BomTreeResult(root, resolvedDirection, allPartNumbers.size());
    }

    public PartFilesResult get(PartFilesCondition condition) {
        currentAuthProvider.getCurrentAuth();
        assertPartExists(condition.partId());

        List<PartFilesResult.Item> items = fileRepository.findByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                        "part",
                        condition.partId(),
                        FileStatus.UPLOADED
                ).stream()
                .map(this::toFileItem)
                .toList();

        return new PartFilesResult(items.size(), items);
    }

    public List<PartFilesResult.Item> getFiles(FileItemsCondition condition) {
        currentAuthProvider.getCurrentAuth();
        return fileRepository.findByIdIn(condition.fileIds()).stream()
                .map(this::toFileItem)
                .toList();
    }

    public PartSuppliersResult get(PartSuppliersCondition condition) {
        currentAuthProvider.getCurrentAuth();
        assertPartExists(condition.partId());

        List<PartSupplier> links = partSupplierRepository.findByPartId(condition.partId());
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

    private PartFilesResult.Item toFileItem(File file) {
        return new PartFilesResult.Item(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getFileSize(),
                fileUrlResolver.resolve(file.getFileKey()),
                file.getCreatedAt()
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

    private RelatedDrawingResult toRelatedDrawing(Drawing drawing) {
        if (drawing == null) {
            return null;
        }
        if (drawing.getDeletedAt() != null) {
            return null;
        }
        return new RelatedDrawingResult(
                drawing.getId(),
                drawing.getDrawingNumber(),
                drawing.getName(),
                drawing.getVersion(),
                drawing.getStatus(),
                drawing.getConversionStatus(),
                fileUrlResolver.resolve(drawing.getThumbnailKey()),
                fileUrlResolver.resolve(drawing.getPdfKey()),
                fileUrlResolver.resolve(drawing.getOriginalFileKey())
        );
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

    private List<BomEdge> fetchBomEdges(UUID rootPartId, boolean reverse) {
        String sql = reverse
                ? """
                        with recursive bom_cte as (
                            select
                                pp.part_number as parent_pn,
                                cp.part_number as child_pn,
                                bl.quantity as quantity,
                                bl.parent_part_id as next_id,
                                1 as depth
                            from bom_links bl
                            join parts pp on pp.id = bl.parent_part_id
                            join parts cp on cp.id = bl.child_part_id
                            where bl.child_part_id = :rootId

                            union all

                            select
                                pp.part_number as parent_pn,
                                cp.part_number as child_pn,
                                bl.quantity as quantity,
                                bl.parent_part_id as next_id,
                                bc.depth + 1 as depth
                            from bom_cte bc
                            join bom_links bl on bl.child_part_id = bc.next_id
                            join parts pp on pp.id = bl.parent_part_id
                            join parts cp on cp.id = bl.child_part_id
                            where bc.depth < :maxDepth
                        )
                        select parent_pn, child_pn, quantity from bom_cte
                        """
                : """
                        with recursive bom_cte as (
                            select
                                pp.part_number as parent_pn,
                                cp.part_number as child_pn,
                                bl.quantity as quantity,
                                bl.child_part_id as next_id,
                                1 as depth
                            from bom_links bl
                            join parts pp on pp.id = bl.parent_part_id
                            join parts cp on cp.id = bl.child_part_id
                            where bl.parent_part_id = :rootId

                            union all

                            select
                                pp.part_number as parent_pn,
                                cp.part_number as child_pn,
                                bl.quantity as quantity,
                                bl.child_part_id as next_id,
                                bc.depth + 1 as depth
                            from bom_cte bc
                            join bom_links bl on bl.parent_part_id = bc.next_id
                            join parts pp on pp.id = bl.parent_part_id
                            join parts cp on cp.id = bl.child_part_id
                            where bc.depth < :maxDepth
                        )
                        select parent_pn, child_pn, quantity from bom_cte
                        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("rootId", rootPartId);
        query.setParameter("maxDepth", MAX_BOM_DEPTH);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new BomEdge(
                        (String) row[0],
                        (String) row[1],
                        row[2] == null ? 1 : ((Number) row[2]).intValue()
                ))
                .toList();
    }

    private BomTreeResult.Node buildBomTree(
            String rootPartNumber,
            List<BomEdge> edges,
            Map<String, Part> partsMap
    ) {
        BomTreeResult.Node rootNode = createBomTreeNode(partsMap.get(rootPartNumber), rootPartNumber, 1);
        Map<String, BomTreeResult.Node> nodeCache = new HashMap<>();
        nodeCache.put(rootPartNumber, rootNode);

        for (BomEdge edge : edges) {
            BomTreeResult.Node parentNode = nodeCache.computeIfAbsent(
                    edge.parentPn(),
                    key -> createBomTreeNode(partsMap.get(key), key, 1)
            );

            String childEdgeKey = edge.parentPn() + "->" + edge.childPn();
            if (nodeCache.containsKey(childEdgeKey)) {
                continue;
            }

            BomTreeResult.Node childNode = createBomTreeNode(
                    partsMap.get(edge.childPn()),
                    edge.childPn(),
                    edge.quantity()
            );
            nodeCache.put(childEdgeKey, childNode);
            nodeCache.putIfAbsent(edge.childPn(), childNode);
            parentNode.children().add(childNode);
        }

        return rootNode;
    }

    private BomTreeResult.Node createBomTreeNode(Part part, String fallbackPartNumber, int quantity) {
        if (part == null) {
            return new BomTreeResult.Node(
                    null,
                    fallbackPartNumber,
                    null,
                    "1",
                    null,
                    null,
                    null,
                    null,
                    quantity,
                    new ArrayList<>()
            );
        }
        return new BomTreeResult.Node(
                part.getId(),
                part.getPartNumber(),
                part.getName(),
                part.getRevision() == null ? "1" : part.getRevision(),
                part.getMaterial(),
                part.getUnit(),
                part.getCategory(),
                part.getLifecycleState(),
                quantity,
                new ArrayList<>()
        );
    }

    private List<Part> findPartsForExport(
            String search,
            String category,
            PartLifecycleState lifecycleState,
            Boolean hasDrawing,
            Boolean hasChildren,
            List<UUID> partIds,
            UUID projectId
    ) {
        PathBuilder<Part> part = new PathBuilder<>(Part.class, "part");
        PathBuilder<BomLink> bomLink = new PathBuilder<>(BomLink.class, "bomLink");
        PathBuilder<ProjectPart> projectPart = new PathBuilder<>(ProjectPart.class, "projectPart");

        BooleanBuilder predicate = buildPartPredicate(
                part,
                bomLink,
                projectPart,
                search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                partIds,
                projectId
        );

        return queryFactory()
                .selectFrom(part)
                .where(predicate)
                .orderBy(part.getString("partNumber").asc())
                .fetch();
    }

    private PartLifecycleState parseLifecycleState(String rawLifecycleState) {
        if (rawLifecycleState == null || rawLifecycleState.isBlank()) {
            return null;
        }
        PartLifecycleState lifecycleState = PartLifecycleState.from(rawLifecycleState);
        if (lifecycleState == null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "lifecycle_state 값이 올바르지 않습니다: " + rawLifecycleState
            );
        }
        return lifecycleState;
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

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private List<String> findDistinctCategories() {
        PathBuilder<Part> part = new PathBuilder<>(Part.class, "part");
        var categoryExpr = part.getString("category");
        return queryFactory()
                .select(categoryExpr)
                .distinct()
                .from(part)
                .where(categoryExpr.isNotNull())
                .orderBy(categoryExpr.asc())
                .fetch();
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
            PathBuilder<BomLink> bomLink,
            PathBuilder<ProjectPart> projectPart,
            String search,
            String category,
            PartLifecycleState lifecycleState,
            Boolean hasDrawing,
            Boolean hasChildren,
            List<UUID> partIds,
            UUID projectId
    ) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (search != null && !search.isBlank()) {
            String keyword = search.trim();
            predicate.and(
                    part.getString("partNumber").containsIgnoreCase(keyword)
                            .or(part.getString("name").containsIgnoreCase(keyword))
            );
        }
        if (category != null && !category.isBlank()) {
            predicate.and(part.getString("category").eq(category));
        }
        if (lifecycleState != null) {
            predicate.and(part.getString("lifecycleState").eq(lifecycleState.value()));
        }
        if (hasDrawing != null) {
            predicate.and(Boolean.TRUE.equals(hasDrawing)
                    ? part.get("drawingId", UUID.class).isNotNull()
                    : part.get("drawingId", UUID.class).isNull());
        }
        if (hasChildren != null) {
            BooleanExpression childExists = JPAExpressions.selectOne()
                    .from(bomLink)
                    .where(bomLink.get("parentPartId", UUID.class).eq(part.get("id", UUID.class)))
                    .exists();
            predicate.and(Boolean.TRUE.equals(hasChildren) ? childExists : childExists.not());
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

    private RelatedDrawingResult loadDrawing(UUID drawingId) {
        if (drawingId == null) {
            return null;
        }
        return drawingRepository.findById(drawingId)
                .map(this::toRelatedDrawing)
                .orElse(null);
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
            int quantity,
            String material,
            String unit,
            String category,
            PartLifecycleState lifecycleState
    ) {
    }

    private record BomEdge(String parentPn, String childPn, int quantity) {
    }
}
