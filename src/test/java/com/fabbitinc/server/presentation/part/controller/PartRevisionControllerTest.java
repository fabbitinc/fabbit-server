package com.fabbitinc.server.presentation.part.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartRevisionLookupByPartCondition;
import com.fabbitinc.server.application.part.query.result.PartRevisionLookupResult;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.presentation.part.response.PartRevisionLookupResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartRevisionControllerTest {

    @Mock
    private PartQuery partQuery;

    @InjectMocks
    private PartRevisionController partRevisionController;

    @Test
    void lookupRevisionsByPart_partId기준_selector목록을_조회한다() {
        UUID partId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();

        when(partQuery.lookupRevisions(any(PartRevisionLookupByPartCondition.class))).thenReturn(new PartRevisionLookupResult(List.of(
                new PartRevisionLookupResult.Item(
                        revisionId,
                        partId,
                        "P-001",
                        "2",
                        "1",
                        "본체",
                        PartRevisionStatus.RELEASED,
                        Instant.parse("2026-04-08T00:00:00Z"),
                        true,
                        null
                )
        )));

        PartRevisionLookupResponse response = partRevisionController.lookupRevisionsByPart(partId);

        ArgumentCaptor<PartRevisionLookupByPartCondition> captor = ArgumentCaptor.forClass(PartRevisionLookupByPartCondition.class);
        verify(partQuery).lookupRevisions(captor.capture());
        assertEquals(partId, captor.getValue().partId());
        assertEquals(1, response.items().size());
        assertEquals("2", response.items().getFirst().revisionCode());
        assertEquals(true, response.items().getFirst().currentReleased());
    }
}
