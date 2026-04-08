package com.fabbitinc.server.application.bom.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.query.condition.WhereUsedSummaryCondition;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WhereUsedSummaryQueryTest {

    @Test
    void get_partId와_revisionId가_일치하지_않으면_예외가_발생한다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        EngineeringBomItemRepository engineeringBomItemRepository = mock(EngineeringBomItemRepository.class);
        PartRevisionRepository partRevisionRepository = mock(PartRevisionRepository.class);
        PartRepository partRepository = mock(PartRepository.class);

        WhereUsedSummaryQuery query = new WhereUsedSummaryQuery(
                currentAuthProvider,
                engineeringBomItemRepository,
                partRevisionRepository,
                partRepository
        );

        UUID actorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(actorId, "a@b.c", orgId, null));
        when(partRevisionRepository.findByIdAndPartId(revisionId, partId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> query.get(new WhereUsedSummaryCondition(partId, revisionId)));

        assertEquals("Part '%s'의 PartRevision '%s'을(를) 찾을 수 없습니다".formatted(partId, revisionId), exception.getMessage());
        verifyNoInteractions(engineeringBomItemRepository, partRepository);
    }

    @Test
    void get_canceled_parent_revision은_whereUsed에서_제외한다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        EngineeringBomItemRepository engineeringBomItemRepository = mock(EngineeringBomItemRepository.class);
        PartRevisionRepository partRevisionRepository = mock(PartRevisionRepository.class);
        PartRepository partRepository = mock(PartRepository.class);

        WhereUsedSummaryQuery query = new WhereUsedSummaryQuery(
                currentAuthProvider,
                engineeringBomItemRepository,
                partRevisionRepository,
                partRepository
        );

        UUID actorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID releasedParentPartId = UUID.randomUUID();
        UUID canceledParentPartId = UUID.randomUUID();
        UUID releasedParentRevisionId = UUID.randomUUID();
        UUID canceledParentRevisionId = UUID.randomUUID();

        PartRevision revision = mock(PartRevision.class);
        when(revision.getId()).thenReturn(revisionId);
        when(revision.getPartId()).thenReturn(partId);

        PartRevision releasedParentRevision = mock(PartRevision.class);
        when(releasedParentRevision.getId()).thenReturn(releasedParentRevisionId);
        when(releasedParentRevision.getPartId()).thenReturn(releasedParentPartId);
        when(releasedParentRevision.getStatus()).thenReturn(PartRevisionStatus.RELEASED);
        when(releasedParentRevision.getRevisionCode()).thenReturn("1");
        when(releasedParentRevision.getName()).thenReturn("released");

        PartRevision canceledParentRevision = mock(PartRevision.class);
        when(canceledParentRevision.getId()).thenReturn(canceledParentRevisionId);
        when(canceledParentRevision.getPartId()).thenReturn(canceledParentPartId);
        when(canceledParentRevision.getStatus()).thenReturn(PartRevisionStatus.CANCELED);
        when(canceledParentRevision.getRevisionCode()).thenReturn(null);
        when(canceledParentRevision.getName()).thenReturn("canceled");

        Part releasedParentPart = mock(Part.class);
        when(releasedParentPart.getId()).thenReturn(releasedParentPartId);
        when(releasedParentPart.getPartNumber()).thenReturn("P-001");

        Part canceledParentPart = mock(Part.class);
        when(canceledParentPart.getId()).thenReturn(canceledParentPartId);
        when(canceledParentPart.getPartNumber()).thenReturn("P-002");

        EngineeringBomItem releasedBomItem = EngineeringBomItem.add(
                releasedParentRevisionId,
                "1",
                revisionId,
                BigDecimal.ONE,
                "{}"
        );
        EngineeringBomItem canceledBomItem = EngineeringBomItem.add(
                canceledParentRevisionId,
                "2",
                revisionId,
                BigDecimal.ONE,
                "{}"
        );

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(actorId, "a@b.c", orgId, null));
        when(partRevisionRepository.findByIdAndPartId(revisionId, partId)).thenReturn(Optional.of(revision));
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(revisionId))
                .thenReturn(List.of(releasedBomItem, canceledBomItem));
        when(partRevisionRepository.findAllById(List.of(releasedParentRevisionId, canceledParentRevisionId)))
                .thenReturn(List.of(releasedParentRevision, canceledParentRevision));
        when(partRepository.findAllById(List.of(releasedParentPartId, canceledParentPartId)))
                .thenReturn(List.of(releasedParentPart, canceledParentPart));

        var result = query.get(new WhereUsedSummaryCondition(partId, revisionId));

        assertEquals(1, result.directReferenceCount());
        assertEquals(1, result.statusBreakdown().releasedCount());
        assertEquals(0, result.statusBreakdown().canceledCount());
        assertEquals(1, result.references().size());
        assertEquals("P-001", result.references().getFirst().partNumber());
    }
}
