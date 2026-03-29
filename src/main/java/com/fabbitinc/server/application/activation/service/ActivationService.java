package com.fabbitinc.server.application.activation.service;

import com.fabbitinc.server.application.activation.service.output.GraphQueryOutput;
import com.fabbitinc.server.application.activation.service.output.GraphQueryResultOutput;
import com.fabbitinc.server.application.activation.service.output.HealthCheckIssueOutput;
import com.fabbitinc.server.application.activation.service.output.HealthCheckOutput;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivationService {

    private final PartRepository partRepository;
    private final PartApi partApi;
    private final DrawingRepository drawingRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartSupplierRepository partSupplierRepository;
    private final ProjectPartRepository projectPartRepository;
    private final EntityManager entityManager;

    public HealthCheckOutput healthCheck() {
        int partCount = safeToInt(partRepository.count());
        int drawingCount = safeToInt(drawingRepository.count());
        int supplierCount = safeToInt(supplierRepository.count());
        int projectCount = safeToInt(projectRepository.countByDeletedFalse());

        Map<String, Integer> nodeCounts = new LinkedHashMap<>();
        nodeCounts.put("Part", partCount);
        nodeCounts.put("Drawing", drawingCount);
        nodeCounts.put("Supplier", supplierCount);
        nodeCounts.put("Project", projectCount);

        int consistsOf = safeToInt(engineeringBomItemRepository.count());
        int definedBy = safeToInt(drawingRepository.countByPartRevisionIdIsNotNullAndDeletedAtIsNull());
        int suppliedBy = safeToInt(partSupplierRepository.count());
        int hasItem = safeToInt(projectPartRepository.count());

        Map<String, Integer> relationshipCounts = new LinkedHashMap<>();
        relationshipCounts.put("CONSISTS_OF", consistsOf);
        relationshipCounts.put("DEFINED_BY", definedBy);
        relationshipCounts.put("SUPPLIED_BY", suppliedBy);
        relationshipCounts.put("HAS_ITEM", hasItem);

        int totalNodes = nodeCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalRelationships = relationshipCounts.values().stream().mapToInt(Integer::intValue).sum();

        List<HealthCheckIssueOutput> issues = new ArrayList<>();
        if (totalNodes == 0) {
            issues.add(new HealthCheckIssueOutput(
                    "empty_graph",
                    "warning",
                    "지식 그래프에 데이터가 없습니다. 먼저 데이터를 합성해주세요.",
                    0
            ));
            return new HealthCheckOutput(
                    totalNodes,
                    totalRelationships,
                    nodeCounts,
                    relationshipCounts,
                    issues
            );
        }

        Set<UUID> connectedParts = new HashSet<>(findDistinctChildPartIds());
        connectedParts.addAll(findDistinctProjectPartIds());
        int orphanParts = Math.max(0, partCount - connectedParts.size());
        if (orphanParts > 0) {
            issues.add(new HealthCheckIssueOutput(
                    "orphan_parts",
                    "warning",
                    "어떤 프로젝트나 조립체에도 소속되지 않은 부품 " + orphanParts + "개",
                    orphanParts
            ));
        }

        int noDrawingParts = safeToInt(countPartsWithoutDrawing());
        if (noDrawingParts > 0) {
            issues.add(new HealthCheckIssueOutput(
                    "missing_drawing",
                    "info",
                    "도면이 연결되지 않은 부품 " + noDrawingParts + "개",
                    noDrawingParts
            ));
        }

        int withSupplierPartCount = safeToInt(countDistinctSuppliedPartIds());
        int noSupplierParts = Math.max(0, partCount - withSupplierPartCount);
        if (noSupplierParts > 0) {
            issues.add(new HealthCheckIssueOutput(
                    "missing_supplier",
                    "info",
                    "공급사가 연결되지 않은 부품 " + noSupplierParts + "개",
                    noSupplierParts
            ));
        }

        int incompleteBom = safeToInt(countChildLinksWithUnnamedPart());
        if (incompleteBom > 0) {
            issues.add(new HealthCheckIssueOutput(
                    "incomplete_bom",
                    "warning",
                    "BOM에 존재하지만 품명 정보가 없는 부품 " + incompleteBom + "개",
                    incompleteBom
            ));
        }

        return new HealthCheckOutput(
                totalNodes,
                totalRelationships,
                nodeCounts,
                relationshipCounts,
                issues
        );
    }

    public GraphQueryOutput queryGraph(String question) {
        if (question == null || question.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "question은 비어 있을 수 없습니다");
        }

        String normalized = question.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("도면") && (normalized.contains("없") || normalized.contains("미연결"))) {
            return queryPartsWithoutDrawing();
        }
        if (normalized.contains("공급사") && normalized.contains("부품")) {
            return querySupplierPartCounts();
        }
        if (normalized.contains("프로젝트") && (normalized.contains("부품 수") || normalized.contains("부품수"))) {
            return queryProjectPartCounts();
        }

        return queryPartsByKeyword(question.trim());
    }

    private GraphQueryOutput queryPartsWithoutDrawing() {
        @SuppressWarnings("unchecked")
        List<UUID> partIds = entityManager.createNativeQuery(
                """
                        select p.id
                        from parts p
                        where not exists (
                            select 1
                            from part_revisions pr
                            join drawings d on d.part_revision_id = pr.id
                            where pr.part_id = p.id
                              and d.deleted_at is null
                        )
                        order by p.part_number asc
                        limit 20
                        """
        ).getResultList();
        List<GraphQueryResultOutput> results = partApi.getPartSnapshotsByIdsOrdered(partIds).stream()
                .map(part -> new GraphQueryResultOutput(
                        "part",
                        part.partNumber(),
                        part.name(),
                        "도면 미연결 부품",
                        null
                ))
                .toList();
        String answer = "도면이 연결되지 않은 부품 " + results.size() + "건을 조회했습니다.";
        return new GraphQueryOutput(results, answer);
    }

    private GraphQueryOutput querySupplierPartCounts() {
        Query query = entityManager.createNativeQuery(
                """
                        select s.company_name, count(distinct pr.part_id) as part_count
                        from suppliers s
                        left join part_suppliers ps on ps.supplier_id = s.id
                        left join part_revisions pr on pr.id = ps.part_revision_id
                        group by s.id, s.company_name
                        order by s.company_name
                        limit 20
                        """
        );
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<GraphQueryResultOutput> results = rows.stream()
                .map(row -> new GraphQueryResultOutput(
                        "supplier",
                        (String) row[0],
                        (String) row[0],
                        "연결 부품 수",
                        ((Number) row[1]).longValue()
                ))
                .toList();
        String answer = "공급사별 연결 부품 수 상위 " + results.size() + "건을 조회했습니다.";
        return new GraphQueryOutput(results, answer);
    }

    private GraphQueryOutput queryProjectPartCounts() {
        Query query = entityManager.createNativeQuery(
                """
                        select p.name, count(pp.part_id) as part_count
                        from projects p
                        left join project_parts pp on pp.project_id = p.id
                        where p.is_deleted = false
                        group by p.id, p.name
                        order by p.name
                        limit 20
                        """
        );
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<GraphQueryResultOutput> results = rows.stream()
                .map(row -> new GraphQueryResultOutput(
                        "project",
                        (String) row[0],
                        (String) row[0],
                        "연결 부품 수",
                        ((Number) row[1]).longValue()
                ))
                .toList();
        String answer = "프로젝트별 부품 수 " + results.size() + "건을 조회했습니다.";
        return new GraphQueryOutput(results, answer);
    }

    private GraphQueryOutput queryPartsByKeyword(String keyword) {
        List<GraphQueryResultOutput> results = partApi.searchPartSnapshots(keyword, 20).stream()
                .map(part -> new GraphQueryResultOutput(
                        "part",
                        part.partNumber(),
                        part.name(),
                        part.description(),
                        null
                ))
                .toList();

        String answer = results.isEmpty()
                ? "질문과 일치하는 결과를 찾지 못했습니다."
                : "질문과 연관된 부품 " + results.size() + "건을 조회했습니다.";
        return new GraphQueryOutput(results, answer);
    }

    private int safeToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private List<UUID> findDistinctChildPartIds() {
        return entityManager.createQuery(
                        "select distinct pr.partId from EngineeringBomItem b join PartRevision pr on pr.id = b.childPartRevisionId",
                        UUID.class
                )
                .getResultList();
    }

    private List<UUID> findDistinctProjectPartIds() {
        return entityManager.createQuery(
                        "select distinct pp.partId from ProjectPart pp",
                        UUID.class
                )
                .getResultList();
    }

    private long countChildLinksWithUnnamedPart() {
        return partApi.getPartSnapshotsByIdsOrdered(findDistinctChildPartIds()).stream()
                .filter(part -> part.name() == null || part.name().isBlank())
                .count();
    }

    private long countDistinctSuppliedPartIds() {
        Number count = (Number) entityManager.createQuery(
                "select count(distinct pr.partId) from PartSupplier ps join PartRevision pr on pr.id = ps.partRevisionId"
        ).getSingleResult();
        return count.longValue();
    }

    private long countPartsWithoutDrawing() {
        Number count = (Number) entityManager.createNativeQuery(
                """
                        select count(*)
                        from parts p
                        where not exists (
                            select 1
                            from part_revisions pr
                            join drawings d on d.part_revision_id = pr.id
                            where pr.part_id = p.id
                              and d.deleted_at is null
                        )
                        """
        ).getSingleResult();
        return count.longValue();
    }
}
