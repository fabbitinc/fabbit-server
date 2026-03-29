package com.fabbitinc.server.application.part.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.part.query.condition.PartImpactAnalysisCondition;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartImpactAnalysisQueryTest {

    @Mock private PartQuery partQuery;
    @Mock private PartRepository partRepository;
    @Mock private PartRevisionRepository partRevisionRepository;
    @Mock private ProjectPartRepository projectPartRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private IssueApi issueApi;

    private PartImpactAnalysisQuery query;

    @BeforeEach
    void setUp() {
        query = new PartImpactAnalysisQuery(
                partQuery,
                partRepository,
                partRevisionRepository,
                projectPartRepository,
                projectRepository,
                issueApi
        );
    }

    @Test
    void 부품이_없으면_예외() {
        UUID partId = UUID.randomUUID();
        when(partRepository.findById(partId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> query.analyze(new PartImpactAnalysisCondition(partId)));
    }

    @Test
    void 리비전이_없으면_빈결과() {
        UUID partId = UUID.randomUUID();
        when(partRepository.findById(partId)).thenReturn(Optional.of(mock(Part.class)));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId)).thenReturn(List.of());

        var result = query.analyze(new PartImpactAnalysisCondition(partId));

        assertTrue(result.bomItems().isEmpty());
        assertEquals(0, result.summary().affectedBomCount());
        assertEquals(0, result.summary().totalCount());
    }

    @Test
    void 상위_BOM이_없으면_빈결과() {
        UUID partId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        PartRevision latestRevision = mock(PartRevision.class);
        when(latestRevision.getId()).thenReturn(revisionId);

        when(partRepository.findById(partId)).thenReturn(Optional.of(mock(Part.class)));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId)).thenReturn(List.of(latestRevision));
        when(partQuery.fetchBomEdges(revisionId, true, 5)).thenReturn(List.of());

        var result = query.analyze(new PartImpactAnalysisCondition(partId));

        assertTrue(result.bomItems().isEmpty());
        assertEquals(0, result.summary().affectedBomCount());
    }

    @Test
    void 영향_항목이_200건을_넘으면_truncated_true() {
        UUID partId = UUID.randomUUID();
        UUID rootRevisionId = UUID.randomUUID();
        PartRevision latestRevision = mock(PartRevision.class);
        when(latestRevision.getId()).thenReturn(rootRevisionId);

        when(partRepository.findById(partId)).thenReturn(Optional.of(mock(Part.class)));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId)).thenReturn(List.of(latestRevision));

        List<PartQuery.BomEdge> edges = new ArrayList<>();
        List<PartRevision> parentRevisions = new ArrayList<>();
        List<Part> parentParts = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            UUID parentRevisionId = UUID.randomUUID();
            UUID parentPartId = UUID.randomUUID();
            edges.add(new PartQuery.BomEdge(parentRevisionId, rootRevisionId, String.valueOf(i), BigDecimal.ONE));

            PartRevision revision = mock(PartRevision.class);
            when(revision.getId()).thenReturn(parentRevisionId);
            when(revision.getPartId()).thenReturn(parentPartId);
            when(revision.getRevisionCode()).thenReturn("A" + i);
            when(revision.getName()).thenReturn("name-" + i);
            parentRevisions.add(revision);

            Part part = mock(Part.class);
            when(part.getId()).thenReturn(parentPartId);
            when(part.getPartNumber()).thenReturn("P-" + i);
            parentParts.add(part);
        }

        when(partQuery.fetchBomEdges(rootRevisionId, true, 5)).thenReturn(edges);
        when(partRevisionRepository.findAllById(any())).thenReturn(parentRevisions.subList(0, 200));
        when(partRepository.findAllById(any())).thenReturn(parentParts.subList(0, 200));
        when(projectPartRepository.findByPartIdIn(any())).thenReturn(List.of());
        when(issueApi.getIssueIdsByPartIds(any())).thenReturn(Set.of());

        var result = query.analyze(new PartImpactAnalysisCondition(partId));

        assertTrue(result.summary().truncated());
        assertEquals(201, result.summary().totalCount());
        assertEquals(200, result.bomItems().size());
    }

    @Test
    void 추천_리뷰어는_중복제거후_최대_5명() {
        UUID partId = UUID.randomUUID();
        UUID rootRevisionId = UUID.randomUUID();
        UUID parentRevisionId = UUID.randomUUID();
        UUID parentPartId = UUID.randomUUID();

        PartRevision latestRevision = mock(PartRevision.class);
        when(latestRevision.getId()).thenReturn(rootRevisionId);

        PartRevision parentRevision = mock(PartRevision.class);
        when(parentRevision.getId()).thenReturn(parentRevisionId);
        when(parentRevision.getPartId()).thenReturn(parentPartId);
        when(parentRevision.getRevisionCode()).thenReturn("B");
        when(parentRevision.getName()).thenReturn("parent");
        when(parentRevision.getStatus()).thenReturn(PartRevisionStatus.DRAFT);

        Part parentPart = mock(Part.class);
        when(parentPart.getId()).thenReturn(parentPartId);
        when(parentPart.getPartNumber()).thenReturn("P-002");

        when(partRepository.findById(partId)).thenReturn(Optional.of(mock(Part.class)));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId)).thenReturn(List.of(latestRevision));
        when(partQuery.fetchBomEdges(rootRevisionId, true, 5)).thenReturn(List.of(new PartQuery.BomEdge(parentRevisionId, rootRevisionId, "1", BigDecimal.ONE)));
        when(partRevisionRepository.findAllById(any())).thenReturn(List.of(parentRevision));
        when(partRepository.findAllById(any())).thenReturn(List.of(parentPart));
        when(projectPartRepository.findByPartIdIn(any())).thenReturn(List.of());
        when(issueApi.getIssueIdsByPartIds(any())).thenReturn(new LinkedHashSet<>(List.of(UUID.randomUUID(), UUID.randomUUID())));
        when(issueApi.getIssueAssigneeUserIds(any())).thenReturn(new LinkedHashSet<>(List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        )));

        var result = query.analyze(new PartImpactAnalysisCondition(partId));

        assertEquals(1, result.summary().draftRevisionCount());
        assertEquals(5, result.summary().suggestedReviewerIds().size());
    }
}
