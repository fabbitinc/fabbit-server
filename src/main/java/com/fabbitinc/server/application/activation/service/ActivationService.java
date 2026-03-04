package com.fabbitinc.server.application.activation.service;

import com.fabbitinc.server.application.activation.dto.response.HealthCheckIssueResponse;
import com.fabbitinc.server.application.activation.dto.response.HealthCheckResponse;
import com.fabbitinc.server.application.activation.dto.response.QueryResponse;
import com.fabbitinc.server.application.activation.dto.response.QueryResultResponse;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActivationService {

    private final PartRepository partRepository;
    private final DrawingRepository drawingRepository;
    private final SupplierRepository supplierRepository;
    private final ProjectRepository projectRepository;
    private final BomLinkRepository bomLinkRepository;
    private final PartSupplierRepository partSupplierRepository;
    private final ProjectPartRepository projectPartRepository;
    private final EntityManager entityManager;

    public HealthCheckResponse healthCheck() {
        int partCount = safeToInt(partRepository.count());
        int drawingCount = safeToInt(drawingRepository.count());
        int supplierCount = safeToInt(supplierRepository.count());
        int projectCount = safeToInt(projectRepository.countByDeletedFalse());

        Map<String, Integer> nodeCounts = new LinkedHashMap<>();
        nodeCounts.put("Part", partCount);
        nodeCounts.put("Drawing", drawingCount);
        nodeCounts.put("Supplier", supplierCount);
        nodeCounts.put("Project", projectCount);

        int consistsOf = safeToInt(bomLinkRepository.count());
        int definedBy = Math.max(0, partCount - safeToInt(partRepository.countByDrawingIdIsNull()));
        int suppliedBy = safeToInt(partSupplierRepository.count());
        int hasItem = safeToInt(projectPartRepository.count());

        Map<String, Integer> relationshipCounts = new LinkedHashMap<>();
        relationshipCounts.put("CONSISTS_OF", consistsOf);
        relationshipCounts.put("DEFINED_BY", definedBy);
        relationshipCounts.put("SUPPLIED_BY", suppliedBy);
        relationshipCounts.put("HAS_ITEM", hasItem);

        int totalNodes = nodeCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalRelationships = relationshipCounts.values().stream().mapToInt(Integer::intValue).sum();

        List<HealthCheckIssueResponse> issues = new ArrayList<>();
        if (totalNodes == 0) {
            issues.add(new HealthCheckIssueResponse(
                    "empty_graph",
                    "warning",
                    "지식 그래프에 데이터가 없습니다. 먼저 데이터를 합성해주세요.",
                    0
            ));
            return new HealthCheckResponse(
                    totalNodes,
                    totalRelationships,
                    nodeCounts,
                    relationshipCounts,
                    issues
            );
        }

        Set<UUID> connectedParts = new HashSet<>(bomLinkRepository.findDistinctChildPartIds());
        connectedParts.addAll(projectPartRepository.findDistinctPartIds());
        int orphanParts = Math.max(0, partCount - connectedParts.size());
        if (orphanParts > 0) {
            issues.add(new HealthCheckIssueResponse(
                    "orphan_parts",
                    "warning",
                    "어떤 프로젝트나 조립체에도 소속되지 않은 부품 " + orphanParts + "개",
                    orphanParts
            ));
        }

        int noDrawingParts = safeToInt(partRepository.countByDrawingIdIsNull());
        if (noDrawingParts > 0) {
            issues.add(new HealthCheckIssueResponse(
                    "missing_drawing",
                    "info",
                    "도면이 연결되지 않은 부품 " + noDrawingParts + "개",
                    noDrawingParts
            ));
        }

        int withSupplierPartCount = safeToInt(partSupplierRepository.countDistinctPartIds());
        int noSupplierParts = Math.max(0, partCount - withSupplierPartCount);
        if (noSupplierParts > 0) {
            issues.add(new HealthCheckIssueResponse(
                    "missing_supplier",
                    "info",
                    "공급사가 연결되지 않은 부품 " + noSupplierParts + "개",
                    noSupplierParts
            ));
        }

        int incompleteBom = safeToInt(bomLinkRepository.countChildLinksWithUnnamedPart());
        if (incompleteBom > 0) {
            issues.add(new HealthCheckIssueResponse(
                    "incomplete_bom",
                    "warning",
                    "BOM에 존재하지만 품명 정보가 없는 부품 " + incompleteBom + "개",
                    incompleteBom
            ));
        }

        return new HealthCheckResponse(
                totalNodes,
                totalRelationships,
                nodeCounts,
                relationshipCounts,
                issues
        );
    }

    public QueryResponse queryGraph(String question) {
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

    private QueryResponse queryPartsWithoutDrawing() {
        List<QueryResultResponse> results = partRepository.findTop20ByDrawingIdIsNullOrderByPartNumberAsc().stream()
                .map(part -> new QueryResultResponse(
                        "part",
                        part.getPartNumber(),
                        part.getName(),
                        "도면 미연결 부품",
                        null
                ))
                .toList();
        String answer = "도면이 연결되지 않은 부품 " + results.size() + "건을 조회했습니다.";
        return new QueryResponse(results, answer);
    }

    private QueryResponse querySupplierPartCounts() {
        Query query = entityManager.createNativeQuery(
                """
                        select s.company_name, count(ps.part_id) as part_count
                        from suppliers s
                        left join part_suppliers ps on ps.supplier_id = s.id
                        group by s.id, s.company_name
                        order by s.company_name
                        limit 20
                        """
        );
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<QueryResultResponse> results = rows.stream()
                .map(row -> new QueryResultResponse(
                        "supplier",
                        (String) row[0],
                        (String) row[0],
                        "연결 부품 수",
                        ((Number) row[1]).longValue()
                ))
                .toList();
        String answer = "공급사별 연결 부품 수 상위 " + results.size() + "건을 조회했습니다.";
        return new QueryResponse(results, answer);
    }

    private QueryResponse queryProjectPartCounts() {
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

        List<QueryResultResponse> results = rows.stream()
                .map(row -> new QueryResultResponse(
                        "project",
                        (String) row[0],
                        (String) row[0],
                        "연결 부품 수",
                        ((Number) row[1]).longValue()
                ))
                .toList();
        String answer = "프로젝트별 부품 수 " + results.size() + "건을 조회했습니다.";
        return new QueryResponse(results, answer);
    }

    private QueryResponse queryPartsByKeyword(String keyword) {
        List<Part> parts = partRepository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
                keyword,
                keyword,
                PageRequest.of(0, 20)
        );
        List<QueryResultResponse> results = parts.stream()
                .map(part -> new QueryResultResponse(
                        "part",
                        part.getPartNumber(),
                        part.getName(),
                        part.getCategory(),
                        null
                ))
                .toList();

        String answer = results.isEmpty()
                ? "질문과 일치하는 결과를 찾지 못했습니다."
                : "질문과 연관된 부품 " + results.size() + "건을 조회했습니다.";
        return new QueryResponse(results, answer);
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
}
