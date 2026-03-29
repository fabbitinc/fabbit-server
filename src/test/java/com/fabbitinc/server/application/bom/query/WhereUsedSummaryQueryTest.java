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
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
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
}
