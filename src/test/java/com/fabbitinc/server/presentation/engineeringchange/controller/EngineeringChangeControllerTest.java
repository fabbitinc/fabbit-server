package com.fabbitinc.server.presentation.engineeringchange.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.engineeringchange.query.EngineeringChangeQuery;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeDetailCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeDetailResult;
import com.fabbitinc.server.application.engineeringchange.usecase.AddEngineeringChangeFilesUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeReviewUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CancelEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeCommentUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.DeleteEngineeringChangeCommentUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.DeleteEngineeringChangeFileUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.RejectEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReleaseEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SubmitEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeLabelsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeStepsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeAffectedItemsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncIssuesUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.UpdateEngineeringChangeCommentUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.UpdateEngineeringChangeUseCase;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.CreateEngineeringChangeRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.EngineeringChangeStepRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.SyncLabelsRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.SyncEngineeringChangeStepsRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeResponse;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;

@ExtendWith(MockitoExtension.class)
class EngineeringChangeControllerTest {

    @Mock
    private EngineeringChangeQuery engineeringChangeQuery;
    @Mock
    private CreateEngineeringChangeUseCase createEngineeringChangeUseCase;
    @Mock
    private UpdateEngineeringChangeUseCase updateEngineeringChangeUseCase;
    @Mock
    private SubmitEngineeringChangeUseCase submitEngineeringChangeUseCase;
    @Mock
    private ApproveEngineeringChangeReviewUseCase approveEngineeringChangeReviewUseCase;
    @Mock
    private RejectEngineeringChangeUseCase rejectEngineeringChangeUseCase;
    @Mock
    private ApproveEngineeringChangeUseCase approveEngineeringChangeUseCase;
    @Mock
    private ReleaseEngineeringChangeUseCase releaseEngineeringChangeUseCase;
    @Mock
    private CancelEngineeringChangeUseCase cancelEngineeringChangeUseCase;
    @Mock
    private SyncIssuesUseCase syncIssuesUseCase;
    @Mock
    private SyncEngineeringChangeLabelsUseCase syncEngineeringChangeLabelsUseCase;
    @Mock
    private SyncEngineeringChangeStepsUseCase syncEngineeringChangeStepsUseCase;
    @Mock
    private SyncEngineeringChangeAffectedItemsUseCase syncEngineeringChangeAffectedItemsUseCase;
    @Mock
    private CreateEngineeringChangeCommentUseCase createEngineeringChangeCommentUseCase;
    @Mock
    private UpdateEngineeringChangeCommentUseCase updateEngineeringChangeCommentUseCase;
    @Mock
    private DeleteEngineeringChangeCommentUseCase deleteEngineeringChangeCommentUseCase;
    @Mock
    private AddEngineeringChangeFilesUseCase addEngineeringChangeFilesUseCase;
    @Mock
    private DeleteEngineeringChangeFileUseCase deleteEngineeringChangeFileUseCase;

    @InjectMocks
    private EngineeringChangeController engineeringChangeController;

    @Test
    void createEngineeringChange_sourceIssue응답이없어도_상세응답을반환한다() {
        UUID engineeringChangeId = UUID.randomUUID();
        Instant now = Instant.parse("2026-03-25T00:00:00Z");
        CreateEngineeringChangeRequest request = new CreateEngineeringChangeRequest(
                "변경관리",
                JsonNodeFactory.instance.objectNode(),
                UUID.randomUUID(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        EngineeringChangeDetailResult detail = new EngineeringChangeDetailResult(
                engineeringChangeId,
                101,
                "변경관리",
                JsonNodeFactory.instance.objectNode(),
                EngineeringChangeState.DRAFT,
                null,
                now,
                now,
                false,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                null,
                null,
                List.of()
        );

        when(createEngineeringChangeUseCase.execute(any()))
                .thenReturn(new CreateEngineeringChangeUseCase.CreateEngineeringChangeResult(engineeringChangeId));
        when(engineeringChangeQuery.getEngineeringChange(any(EngineeringChangeDetailCondition.class)))
                .thenReturn(detail);

        EngineeringChangeResponse response = engineeringChangeController.createEngineeringChange(request);

        assertEquals(engineeringChangeId, response.id());
        assertNull(response.sourceIssue());
        verify(createEngineeringChangeUseCase).execute(any());
        verify(engineeringChangeQuery).getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId));
    }

    @Test
    void syncSteps_기존StageId를포함한명령을_usecase로전달한다() {
        UUID engineeringChangeId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        EngineeringChangeDetailResult detail = new EngineeringChangeDetailResult(
                engineeringChangeId,
                101,
                "변경관리",
                JsonNodeFactory.instance.objectNode(),
                EngineeringChangeState.DRAFT,
                null,
                Instant.parse("2026-03-25T00:00:00Z"),
                Instant.parse("2026-03-25T00:00:00Z"),
                false,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                null,
                null,
                List.of()
        );
        SyncEngineeringChangeStepsRequest request = new SyncEngineeringChangeStepsRequest(List.of(
                new EngineeringChangeStepRequest(
                        stageId,
                        EngineeringChangeStepType.REVIEW,
                        1,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE,
                        null,
                        null,
                        List.of(new EngineeringChangeStepRequest.AssigneeRequest(
                                EngineeringChangeStepAssigneeType.USER,
                                UUID.randomUUID()
                        ))
                )
        ));

        when(engineeringChangeQuery.getEngineeringChange(any(EngineeringChangeDetailCondition.class)))
                .thenReturn(detail);

        EngineeringChangeResponse response = engineeringChangeController.syncSteps(engineeringChangeId, request);

        assertEquals(engineeringChangeId, response.id());
        verify(syncEngineeringChangeStepsUseCase).execute(any());
        verify(engineeringChangeQuery).getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId));
    }

    @Test
    void syncLabels_라벨목록을_usecase로전달한다() {
        UUID engineeringChangeId = UUID.randomUUID();
        SyncLabelsRequest request = new SyncLabelsRequest(List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(syncEngineeringChangeLabelsUseCase.execute(any())).thenReturn(new SyncDiffResult(1, 0));

        engineeringChangeController.syncLabels(engineeringChangeId, request);

        verify(syncEngineeringChangeLabelsUseCase).execute(any());
    }
}
