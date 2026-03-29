package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.part.query.condition.PartImpactAnalysisCondition;
import com.fabbitinc.server.application.part.query.result.PartImpactAnalysisResult;
import com.fabbitinc.server.application.part.query.result.PartImpactAnalysisResult.AffectedBomItem;
import com.fabbitinc.server.application.part.query.result.PartImpactAnalysisResult.AffectedProject;
import com.fabbitinc.server.application.part.query.result.PartImpactAnalysisResult.Summary;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부품 영향 분석 조회 Query.
 * 특정 부품을 변경했을 때 영향받는 상위 BOM, 프로젝트, 리뷰어 정보를 분석한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartImpactAnalysisQuery {

    private static final int MAX_BOM_DEPTH = 5;
    private static final int MAX_AFFECTED_ITEMS = 200;
    private static final int MAX_SUGGESTED_REVIEWERS = 5;

    private final PartQuery partQuery;
    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final ProjectPartRepository projectPartRepository;
    private final ProjectRepository projectRepository;
    private final IssueApi issueApi;

    public PartImpactAnalysisResult analyze(PartImpactAnalysisCondition condition) {
        // 1. 부품 존재 확인
        partRepository.findById(condition.partId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // 2. 최신 리비전 조회
        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(condition.partId());
        if (revisions.isEmpty()) {
            return emptyResult();
        }
        PartRevision latestRevision = revisions.getFirst();

        // 3. 역방향 BOM 탐색 (상위 부품 조회)
        List<PartQuery.BomEdge> edges = partQuery.fetchBomEdges(latestRevision.getId(), true, MAX_BOM_DEPTH);
        if (edges.isEmpty()) {
            return emptyResult();
        }

        // 4. 영향받는 상위 리비전 ID 수집 및 절삭 처리
        Set<UUID> parentRevisionIds = edges.stream()
                .map(PartQuery.BomEdge::parentRevisionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int totalCount = parentRevisionIds.size();
        boolean truncated = totalCount > MAX_AFFECTED_ITEMS;
        if (truncated) {
            parentRevisionIds = parentRevisionIds.stream()
                    .limit(MAX_AFFECTED_ITEMS)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        // 5. 리비전/부품 일괄 로딩
        Map<UUID, PartRevision> revisionsById = partRevisionRepository.findAllById(parentRevisionIds).stream()
                .collect(Collectors.toMap(PartRevision::getId, Function.identity()));
        Set<UUID> partIds = revisionsById.values().stream()
                .map(PartRevision::getPartId)
                .collect(Collectors.toSet());
        Map<UUID, Part> partsById = partRepository.findAllById(partIds).stream()
                .collect(Collectors.toMap(Part::getId, Function.identity()));

        // 6. 레벨 계산 (BFS 기반 최소 깊이)
        Map<UUID, Integer> depthByRevisionId = calculateMinDepth(edges, latestRevision.getId());

        // 7. AffectedBomItem 목록 생성
        List<AffectedBomItem> bomItems = parentRevisionIds.stream()
                .filter(revisionsById::containsKey)
                .map(revId -> {
                    PartRevision rev = revisionsById.get(revId);
                    Part part = partsById.get(rev.getPartId());
                    int level = depthByRevisionId.getOrDefault(revId, 1);
                    return new AffectedBomItem(
                            part.getId(),
                            part.getPartNumber(),
                            rev.getName(),
                            rev.getRevisionCode(),
                            level
                    );
                })
                .toList();

        // 8. 프로젝트 조회
        List<ProjectPart> projectParts = projectPartRepository.findByPartIdIn(partIds);
        Set<UUID> projectIds = projectParts.stream()
                .map(ProjectPart::getProjectId)
                .collect(Collectors.toSet());
        List<AffectedProject> projects = List.of();
        if (!projectIds.isEmpty()) {
            Map<UUID, Project> projectsById = projectRepository.findAllById(projectIds).stream()
                    .collect(Collectors.toMap(Project::getId, Function.identity()));
            projects = projectIds.stream()
                    .filter(projectsById::containsKey)
                    .map(pid -> new AffectedProject(pid, projectsById.get(pid).getName()))
                    .toList();
        }

        // 9. DRAFT 리비전 개수
        int draftRevisionCount = (int) revisionsById.values().stream()
                .filter(rev -> rev.getStatus() == PartRevisionStatus.DRAFT)
                .count();

        // 10. 추천 리뷰어 조회
        List<UUID> suggestedReviewerIds = collectSuggestedReviewers(partIds);

        Summary summary = new Summary(
                bomItems.size(),
                projects.size(),
                draftRevisionCount,
                suggestedReviewerIds,
                truncated,
                totalCount
        );

        return new PartImpactAnalysisResult(bomItems, projects, summary);
    }

    /**
     * BFS 기반으로 각 상위 리비전의 최소 깊이를 계산한다.
     * 역방향 BOM에서 rootRevisionId를 기준으로 parentRevisionId 방향으로 탐색한다.
     */
    private Map<UUID, Integer> calculateMinDepth(List<PartQuery.BomEdge> edges, UUID rootRevisionId) {
        // 역방향 BOM: childRevisionId → parentRevisionId 방향으로 탐색
        // edges에서 childRevisionId가 현재 노드이고, parentRevisionId가 상위 노드
        Map<UUID, List<PartQuery.BomEdge>> edgesByChild = new HashMap<>();
        for (PartQuery.BomEdge edge : edges) {
            edgesByChild.computeIfAbsent(edge.childRevisionId(), k -> new ArrayList<>()).add(edge);
        }

        Map<UUID, Integer> depthByRevisionId = new HashMap<>();
        Set<UUID> visited = new HashSet<>();
        List<UUID> currentLevel = List.of(rootRevisionId);
        int depth = 0;

        while (!currentLevel.isEmpty()) {
            depth++;
            List<UUID> nextLevel = new ArrayList<>();
            for (UUID nodeId : currentLevel) {
                List<PartQuery.BomEdge> nodeEdges = edgesByChild.getOrDefault(nodeId, List.of());
                for (PartQuery.BomEdge edge : nodeEdges) {
                    UUID parentId = edge.parentRevisionId();
                    if (!visited.contains(parentId)) {
                        visited.add(parentId);
                        depthByRevisionId.put(parentId, depth);
                        nextLevel.add(parentId);
                    }
                }
            }
            currentLevel = nextLevel;
        }

        return depthByRevisionId;
    }

    /**
     * 영향받는 부품과 관련된 이슈 담당자를 추천 리뷰어로 수집한다.
     */
    private List<UUID> collectSuggestedReviewers(Set<UUID> partIds) {
        Set<UUID> issueIds = issueApi.getIssueIdsByPartIds(partIds);
        if (issueIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> reviewerIds = new LinkedHashSet<>();
        for (UUID issueId : issueIds) {
            Set<UUID> assignees = issueApi.getIssueAssigneeUserIds(issueId);
            reviewerIds.addAll(assignees);
            if (reviewerIds.size() >= MAX_SUGGESTED_REVIEWERS) {
                break;
            }
        }

        return reviewerIds.stream()
                .limit(MAX_SUGGESTED_REVIEWERS)
                .toList();
    }

    private PartImpactAnalysisResult emptyResult() {
        return new PartImpactAnalysisResult(
                List.of(),
                List.of(),
                new Summary(0, 0, 0, List.of(), false, 0)
        );
    }
}
