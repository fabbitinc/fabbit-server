package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.part.dto.response.BomChildResponse;
import com.fabbitinc.server.application.part.dto.response.BomParentResponse;
import com.fabbitinc.server.application.part.dto.response.BomTreeNodeResponse;
import com.fabbitinc.server.application.part.dto.response.BomTreeResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryLookupResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryStatsItemResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryStatsResponse;
import com.fabbitinc.server.application.part.dto.response.PartDetailResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilterOptionsResponse;
import com.fabbitinc.server.application.part.dto.response.PartBomResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilesResponse;
import com.fabbitinc.server.application.part.dto.response.PartListResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerUserSummaryResponse;
import com.fabbitinc.server.application.part.dto.response.RelatedDrawingResponse;
import com.fabbitinc.server.application.part.dto.response.PartSummaryResponse;
import com.fabbitinc.server.application.part.dto.response.PartSuppliersResponse;
import com.fabbitinc.server.application.part.dto.response.RelatedSupplierResponse;
import com.fabbitinc.server.application.project.api.ProjectApi;
import com.fabbitinc.server.application.project.dto.response.PartProjectsResponse;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.part.model.BomLink;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartSupplier;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final ProjectApi projectApi;
    private final FileUrlResolver fileUrlResolver;
    private final EntityManager entityManager;

    private static final Pattern STRING_PATTERN = Pattern.compile("^\"(.*)\"$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?$");
    private static final int MAX_BOM_DEPTH = 30;

    public PartLookupResponse lookupParts(String search, int limit) {
        currentAuthProvider.getCurrentAuth();

        List<Part> parts;
        if (search == null || search.isBlank()) {
            parts = partRepository.findAllByOrderByPartNumberAsc(PageRequest.of(0, limit));
        } else {
            String normalizedSearch = search.trim();
            parts = partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                    normalizedSearch,
                    normalizedSearch,
                    PageRequest.of(0, limit)
            );
        }

        List<PartLookupItemResponse> items = parts.stream()
                .map(part -> new PartLookupItemResponse(part.getId(), part.getPartNumber(), part.getName()))
                .toList();
        return new PartLookupResponse(items);
    }

    public CategoryStatsResponse listCategories() {
        currentAuthProvider.getCurrentAuth();

        List<CategoryStatsItemResponse> items = partRepository.findCategoryStats().stream()
                .map(row -> new CategoryStatsItemResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
        return new CategoryStatsResponse(items);
    }

    public CategoryLookupResponse lookupCategories() {
        currentAuthProvider.getCurrentAuth();
        return new CategoryLookupResponse(partRepository.findDistinctCategories());
    }

    public PartFilterOptionsResponse getFilterOptions() {
        currentAuthProvider.getCurrentAuth();
        return new PartFilterOptionsResponse(
                partRepository.findDistinctCategories(),
                partRepository.findDistinctLifecycleStates()
        );
    }

    public PartListResponse listParts(String search,
            String category,
            String lifecycleState,
            Boolean hasDrawing,
            Boolean hasChildren,
            UUID projectId,
            int offset,
            int limit
    ) {
        currentAuthProvider.getCurrentAuth();

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
                search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                null,
                projectId
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
                .offset(offset)
                .limit(limit)
                .fetch();

        List<PartSummaryResponse> items = rows.stream()
                .map(row -> new PartSummaryResponse(
                        row.get(partIdExpr),
                        row.get(partNumberExpr),
                        row.get(partNameExpr),
                        row.get(partCategoryExpr),
                        row.get(partRevisionExpr) == null ? "1" : row.get(partRevisionExpr),
                        row.get(partLifecycleStateExpr),
                        row.get(drawingNumberExpr),
                        row.get(childrenCountExpr) == null ? 0L : row.get(childrenCountExpr)
                ))
                .toList();
        return new PartListResponse(total, offset, limit, items);
    }

    public byte[] exportPartsExcel(String search,
            String category,
            String lifecycleState,
            Boolean hasDrawing,
            Boolean hasChildren,
            List<UUID> partIds,
            UUID mappingId,
            UUID projectId
    ) {
        currentAuthProvider.getCurrentAuth();
        List<Part> parts = findPartsForExport(
                search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                partIds,
                projectId
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

    public byte[] exportBomTreeExcel(UUID partId,
            String direction,
            UUID mappingId
    ) {
        BomTreeResponse tree = getBomTree(partId, direction);
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

    public PartDetailResponse getPartDetail(UUID partId) {
        currentAuthProvider.getCurrentAuth();

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'을(를) 찾을 수 없습니다"
                ));

        PartOwnerUserSummaryResponse owner = toUserSummary(part.getOwner());
        String ownerTeamName = part.getOwnerTeam() == null ? null : part.getOwnerTeam().getName();
        RelatedDrawingResponse drawing = toRelatedDrawing(part.getDrawing());

        long childrenCount = bomLinkRepository.countByParentPartId(part.getId());
        long parentsCount = bomLinkRepository.countByChildPartId(part.getId());
        long suppliersCount = partSupplierRepository.countByPartId(part.getId());
        long filesCount = fileRepository.countByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                "part",
                part.getId(),
                FileStatus.UPLOADED
        );
        long projectsCount = projectApi.countPartProjects(part.getId());

        return new PartDetailResponse(
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

    public PartProjectsResponse getPartProjects(UUID partId) {
        currentAuthProvider.getCurrentAuth();
        return projectApi.getPartProjects(partId);
    }

    public PartBomResponse getPartBom(UUID partId) {
        currentAuthProvider.getCurrentAuth();
        assertPartExists(partId);

        Query childrenQuery = entityManager.createNativeQuery(
                """
                        select c.id, c.part_number, c.name, bl.quantity, bl.extended_properties
                        from bom_links bl
                        join parts c on c.id = bl.child_part_id
                        where bl.parent_part_id = :partId
                        order by c.part_number
                        """
        );
        childrenQuery.setParameter("partId", partId);

        Query parentsQuery = entityManager.createNativeQuery(
                """
                        select p.id, p.part_number, p.name, bl.quantity, bl.extended_properties
                        from bom_links bl
                        join parts p on p.id = bl.parent_part_id
                        where bl.child_part_id = :partId
                        order by p.part_number
                        """
        );
        parentsQuery.setParameter("partId", partId);

        @SuppressWarnings("unchecked")
        List<Object[]> childRows = childrenQuery.getResultList();
        @SuppressWarnings("unchecked")
        List<Object[]> parentRows = parentsQuery.getResultList();

        List<BomChildResponse> children = childRows.stream()
                .map(row -> new BomChildResponse(
                        toUuid(row[0]),
                        (String) row[1],
                        (String) row[2],
                        row[3] == null ? 1 : ((Number) row[3]).intValue(),
                        parseExtendedProperties(row[4] == null ? null : row[4].toString())
                ))
                .toList();

        List<BomParentResponse> parents = parentRows.stream()
                .map(row -> new BomParentResponse(
                        toUuid(row[0]),
                        (String) row[1],
                        (String) row[2],
                        row[3] == null ? 1 : ((Number) row[3]).intValue(),
                        parseExtendedProperties(row[4] == null ? null : row[4].toString())
                ))
                .toList();

        return new PartBomResponse(children, parents);
    }

    public BomTreeResponse getBomTree(UUID partId, String direction) {
        currentAuthProvider.getCurrentAuth();

        Part rootPart = partRepository.findById(partId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "Part '" + partId + "'을(를) 찾을 수 없습니다"
                ));

        boolean reverse = resolveDirection(direction);
        List<BomEdge> edges = fetchBomEdges(rootPart.getId(), reverse);

        Set<String> allPartNumbers = new HashSet<>();
        allPartNumbers.add(rootPart.getPartNumber());
        for (BomEdge edge : edges) {
            allPartNumbers.add(edge.parentPn());
            allPartNumbers.add(edge.childPn());
        }

        Map<String, Part> partsMap = partRepository.findByPartNumberIn(allPartNumbers).stream()
                .collect(java.util.stream.Collectors.toMap(Part::getPartNumber, part -> part));
        BomTreeNodeResponse root = buildBomTree(rootPart.getPartNumber(), edges, partsMap);

        return new BomTreeResponse(root, reverse ? "reverse" : "forward", allPartNumbers.size());
    }

    public PartFilesResponse getPartFiles(UUID partId) {
        currentAuthProvider.getCurrentAuth();
        assertPartExists(partId);

        List<FileItemResponse> items = fileRepository.findByOwnerTypeAndOwnerIdAndStatusAndDeletedAtIsNull(
                        "part",
                        partId,
                        FileStatus.UPLOADED
                ).stream()
                .map(this::toFileItem)
                .toList();

        return new PartFilesResponse(items.size(), items);
    }

    public List<FileItemResponse> getFilesByIds(List<UUID> fileIds) {
        currentAuthProvider.getCurrentAuth();
        return fileRepository.findByIdIn(fileIds).stream()
                .map(this::toFileItem)
                .toList();
    }

    public PartSuppliersResponse getPartSuppliers(UUID partId) {
        currentAuthProvider.getCurrentAuth();
        assertPartExists(partId);

        List<PartSupplier> links = partSupplierRepository.findByPartId(partId);
        if (links.isEmpty()) {
            return new PartSuppliersResponse(0, List.of());
        }

        Map<UUID, PartSupplier> linkMap = links.stream()
                .collect(java.util.stream.Collectors.toMap(PartSupplier::getSupplierId, link -> link));

        List<UUID> supplierIds = links.stream().map(PartSupplier::getSupplierId).toList();
        List<RelatedSupplierResponse> items = supplierRepository.findAllById(supplierIds).stream()
                .map(supplier -> toRelatedSupplier(supplier, linkMap.get(supplier.getId())))
                .sorted(Comparator.comparing(RelatedSupplierResponse::companyName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new PartSuppliersResponse(items.size(), items);
    }

    private FileItemResponse toFileItem(File file) {
        return new FileItemResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getFileSize(),
                fileUrlResolver.resolve(file.getFileKey()),
                file.getCreatedAt()
        );
    }

    private PartOwnerUserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new PartOwnerUserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey())
        );
    }

    private RelatedDrawingResponse toRelatedDrawing(Drawing drawing) {
        if (drawing == null) {
            return null;
        }
        if (drawing.getDeletedAt() != null) {
            return null;
        }
        return new RelatedDrawingResponse(
                drawing.getId(),
                drawing.getDrawingNumber(),
                drawing.getName(),
                drawing.getVersion(),
                drawing.getStatus(),
                drawing.getConversionStatus() == null ? null : drawing.getConversionStatus().name(),
                fileUrlResolver.resolve(drawing.getThumbnailKey()),
                fileUrlResolver.resolve(drawing.getPdfKey()),
                fileUrlResolver.resolve(drawing.getOriginalFileKey())
        );
    }

    private RelatedSupplierResponse toRelatedSupplier(Supplier supplier, PartSupplier link) {
        return new RelatedSupplierResponse(
                supplier.getId(),
                supplier.getCompanyName(),
                supplier.getCode(),
                supplier.getCountry(),
                link == null ? null : link.getUnitCost()
        );
    }

    private boolean resolveDirection(String direction) {
        if (direction == null || direction.isBlank() || "forward".equalsIgnoreCase(direction)) {
            return false;
        }
        if ("reverse".equalsIgnoreCase(direction)) {
            return true;
        }
        throw new AppException(ErrorCode.VALIDATION_ERROR, "direction은 forward 또는 reverse 여야 합니다");
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

    private BomTreeNodeResponse buildBomTree(
            String rootPartNumber,
            List<BomEdge> edges,
            Map<String, Part> partsMap
    ) {
        BomTreeNodeResponse rootNode = createBomTreeNode(partsMap.get(rootPartNumber), rootPartNumber, 1);
        Map<String, BomTreeNodeResponse> nodeCache = new HashMap<>();
        nodeCache.put(rootPartNumber, rootNode);

        for (BomEdge edge : edges) {
            BomTreeNodeResponse parentNode = nodeCache.computeIfAbsent(
                    edge.parentPn(),
                    key -> createBomTreeNode(partsMap.get(key), key, 1)
            );

            String childEdgeKey = edge.parentPn() + "->" + edge.childPn();
            if (nodeCache.containsKey(childEdgeKey)) {
                continue;
            }

            BomTreeNodeResponse childNode = createBomTreeNode(
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

    private BomTreeNodeResponse createBomTreeNode(Part part, String fallbackPartNumber, int quantity) {
        if (part == null) {
            return new BomTreeNodeResponse(
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
        return new BomTreeNodeResponse(
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
            String lifecycleState,
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

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    private BooleanBuilder buildPartPredicate(
            PathBuilder<Part> part,
            PathBuilder<BomLink> bomLink,
            PathBuilder<ProjectPart> projectPart,
            String search,
            String category,
            String lifecycleState,
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
        if (lifecycleState != null && !lifecycleState.isBlank()) {
            predicate.and(part.getString("lifecycleState").eq(lifecycleState));
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

    private void flattenBomTree(BomTreeNodeResponse node, int level, List<BomFlatRow> rows) {
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
        for (BomTreeNodeResponse child : node.children()) {
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

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
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
            String lifecycleState
    ) {
    }

    private record BomEdge(String parentPn, String childPn, int quantity) {
    }
}
