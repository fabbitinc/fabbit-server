package com.fabbitinc.server.application.bom.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.query.condition.BomCompareCondition;
import com.fabbitinc.server.application.bom.query.result.BomCompareResult;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BomCompareQueryTest {

    @Test
    void compare_lineNumber가_숫자로_같아도_원본문자열이_다르면_둘다_유지한다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        EngineeringBomItemRepository engineeringBomItemRepository = mock(EngineeringBomItemRepository.class);
        PartRevisionRepository partRevisionRepository = mock(PartRevisionRepository.class);
        PartRepository partRepository = mock(PartRepository.class);

        BomCompareQuery query = new BomCompareQuery(
                currentAuthProvider,
                engineeringBomItemRepository,
                partRevisionRepository,
                partRepository
        );

        UUID actorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(actorId, "a@b.c", orgId, null));

        Part sourcePart = Part.create("SRC-001");
        Part targetPart = Part.create("TGT-001");
        Part childPartA = Part.create("CHILD-A");
        Part childPartB = Part.create("CHILD-B");

        PartRevision sourceRevision = PartRevision.createInitialDraft(sourcePart, "source", actorId);
        PartRevision targetRevision = PartRevision.createInitialDraft(targetPart, "target", actorId);
        PartRevision childRevisionA = PartRevision.createInitialDraft(childPartA, "child-a", actorId);
        PartRevision childRevisionB = PartRevision.createInitialDraft(childPartB, "child-b", actorId);

        when(partRevisionRepository.findById(sourceRevision.getId())).thenReturn(Optional.of(sourceRevision));
        when(partRevisionRepository.findById(targetRevision.getId())).thenReturn(Optional.of(targetRevision));
        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(sourceRevision.getId()))
                .thenReturn(List.of(EngineeringBomItem.add(
                        sourceRevision.getId(),
                        "1",
                        childRevisionA.getId(),
                        BigDecimal.ONE,
                        null
                )));
        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(targetRevision.getId()))
                .thenReturn(List.of(EngineeringBomItem.add(
                        targetRevision.getId(),
                        "01",
                        childRevisionB.getId(),
                        BigDecimal.ONE,
                        null
                )));
        when(partRevisionRepository.findAllById(List.of(childRevisionA.getId(), childRevisionB.getId())))
                .thenReturn(List.of(childRevisionA, childRevisionB));
        when(partRepository.findAllById(List.of(childPartA.getId(), childPartB.getId())))
                .thenReturn(List.of(childPartA, childPartB));

        BomCompareResult result = query.compare(new BomCompareCondition(sourceRevision.getId(), targetRevision.getId()));

        assertEquals(2, result.changes().size());
        assertEquals("01", result.changes().get(0).lineNumber());
        assertEquals("1", result.changes().get(1).lineNumber());
        assertEquals(1, result.summary().addedCount());
        assertEquals(1, result.summary().removedCount());
        assertEquals(0, result.summary().changedCount());
        assertEquals(0, result.summary().unchangedCount());
        assertEquals(2, result.summary().totalCount());
    }
}
