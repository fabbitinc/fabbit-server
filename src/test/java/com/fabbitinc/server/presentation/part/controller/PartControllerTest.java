package com.fabbitinc.server.presentation.part.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.part.query.PartImpactAnalysisQuery;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartRevisionLookupCondition;
import com.fabbitinc.server.application.part.query.result.PartRevisionLookupResult;
import com.fabbitinc.server.application.part.usecase.ChangePartLifecycleStateUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.presentation.part.request.PartRevisionLookupStatusRequest;
import com.fabbitinc.server.presentation.part.response.PartRevisionLookupResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartControllerTest {

    @Mock
    private PartQuery partQuery;
    @Mock
    private PartImpactAnalysisQuery partImpactAnalysisQuery;
    @Mock
    private CreatePartUseCase createPartUseCase;
    @Mock
    private ChangePartLifecycleStateUseCase changePartLifecycleStateUseCase;

    @InjectMocks
    private PartController partController;

    @Test
    void lookupRevisions_명시한_필터를_조건에_담아전달한다() {
        when(partQuery.lookupRevisions(any())).thenReturn(new PartRevisionLookupResult(List.of(
                new PartRevisionLookupResult.Item(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "P-001",
                        "1",
                        "본체",
                        com.fabbitinc.server.domain.part.model.PartRevisionStatus.DRAFT,
                        null
                )
        )));

        PartRevisionLookupResponse response = partController.lookupRevisions(
                "motor",
                PartRevisionLookupStatusRequest.DRAFT,
                false,
                20
        );

        ArgumentCaptor<PartRevisionLookupCondition> captor = ArgumentCaptor.forClass(PartRevisionLookupCondition.class);
        verify(partQuery).lookupRevisions(captor.capture());
        assertEquals("motor", captor.getValue().search());
        assertEquals(20, captor.getValue().limit());
        assertEquals(PartRevisionStatus.DRAFT, captor.getValue().status());
        assertEquals(false, captor.getValue().mineOnly());
        assertEquals(1, response.items().size());
    }
}
