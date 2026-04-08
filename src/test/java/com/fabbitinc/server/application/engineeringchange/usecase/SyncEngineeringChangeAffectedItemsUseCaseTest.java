package com.fabbitinc.server.application.engineeringchange.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SyncEngineeringChangeAffectedItemsUseCaseTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;
    @Mock
    private EngineeringChangeService engineeringChangeService;
    @Mock
    private EngineeringChangeRepository engineeringChangeRepository;
    @Mock
    private EngineeringChangeAffectedItemRepository affectedItemRepository;
    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private PartRepository partRepository;
    @Mock
    private PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    @Mock
    private PopulateWhereUsedAffectedItemsUseCase populateWhereUsedAffectedItemsUseCase;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SyncEngineeringChangeAffectedItemsUseCase useCase;

    @Test
    void 다른_활성EC가_같은_draft를_잡고있으면_conflict를_던진다() {
        UUID actorId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        EngineeringChange currentEc = EngineeringChange.create(10, "현재", "본문", null, actorId);
        EngineeringChange otherEc = EngineeringChange.create(12, "다른", "본문", null, actorId);
        org.springframework.test.util.ReflectionTestUtils.setField(otherEc, "state", EngineeringChangeState.REVIEW_PENDING);

        PartRevision revision = PartRevision.createInitialDraft(Part.create("TEST-001"), "draft", actorId);
        org.springframework.test.util.ReflectionTestUtils.setField(revision, "id", revisionId);

        EngineeringChangeAffectedItem link = EngineeringChangeAffectedItem.create(
                otherEc.getId(),
                EngineeringChangeAffectedItemType.REVISION_RELEASE,
                revisionId,
                null
        );

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                actorId,
                "test@example.com",
                UUID.randomUUID(),
                MembershipRole.OWNER
        ));
        when(engineeringChangeService.getEngineeringChangeByIdOrThrow(currentEc.getId())).thenReturn(currentEc);
        when(partRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(affectedItemRepository.findByTargetIdAndItemTypeOrderByCreatedAtAsc(
                revisionId,
                EngineeringChangeAffectedItemType.REVISION_RELEASE
        )).thenReturn(List.of(link));
        when(engineeringChangeRepository.findAllById(List.of(otherEc.getId()))).thenReturn(List.of(otherEc));

        AppException ex = assertThrows(
                AppException.class,
                () -> useCase.execute(new SyncEngineeringChangeAffectedItemsUseCase.SyncEngineeringChangeAffectedItemsCommand(
                        currentEc.getId(),
                        List.of(new SyncEngineeringChangeAffectedItemsUseCase.Item(
                                EngineeringChangeAffectedItemType.REVISION_RELEASE,
                                revisionId,
                                null
                        ))
                ))
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void sync시_변경대상부품기준_whereUsed를_자동으로_반영한다() {
        UUID actorId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(10, "현재", "본문", null, actorId);
        PartRevision revision = PartRevision.createInitialDraft(Part.create("TEST-001"), "draft", actorId);
        org.springframework.test.util.ReflectionTestUtils.setField(revision, "id", revisionId);

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                actorId,
                "test@example.com",
                UUID.randomUUID(),
                MembershipRole.OWNER
        ));
        when(engineeringChangeService.getEngineeringChangeByIdOrThrow(engineeringChange.getId())).thenReturn(engineeringChange);
        when(partRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
        when(affectedItemRepository.findByTargetIdAndItemTypeOrderByCreatedAtAsc(
                revisionId,
                EngineeringChangeAffectedItemType.REVISION_RELEASE
        )).thenReturn(List.of());
        when(engineeringChangeRepository.findAllById(List.of())).thenReturn(List.of());

        useCase.execute(new SyncEngineeringChangeAffectedItemsUseCase.SyncEngineeringChangeAffectedItemsCommand(
                engineeringChange.getId(),
                List.of(new SyncEngineeringChangeAffectedItemsUseCase.Item(
                        EngineeringChangeAffectedItemType.REVISION_RELEASE,
                        revisionId,
                        null
                ))
        ));

        verify(populateWhereUsedAffectedItemsUseCase).populateForDraftEngineeringChange(engineeringChange);
    }
}
