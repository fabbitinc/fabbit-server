package com.fabbitinc.server.integration.engineeringchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeReviewUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CancelEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.RejectEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReleaseEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.RequestChangesOnStepUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ResubmitStepUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SubmitEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeAffectedItemsUseCase;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.part.usecase.ChangePartLifecycleStateUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.application.settings.usecase.UpdateSettingsPartWorkflowPolicyUseCase;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.StepStageRepository;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.fixture.EngineeringChangeIntegrationFixture;
import com.fabbitinc.server.integration.fixture.EngineeringChangeIntegrationFixture.PartWithReleasedRevision;
import com.fabbitinc.server.integration.support.PostgresIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EngineeringChangeWorkflowIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired private UserRepository userRepository;
    @Autowired private PartRevisionWorkflowPolicyService workflowPolicyService;
    @Autowired private CreatePartUseCase createPartUseCase;
    @Autowired private CreatePartDraftUseCase createPartDraftUseCase;
    @Autowired private ReleasePartDraftUseCase releasePartDraftUseCase;
    @Autowired private ChangePartLifecycleStateUseCase changePartLifecycleStateUseCase;
    @Autowired private UpdateSettingsPartWorkflowPolicyUseCase updateWorkflowPolicyUseCase;
    @Autowired private CreateEngineeringChangeUseCase createEcUseCase;
    @Autowired private SyncEngineeringChangeAffectedItemsUseCase syncAffectedItemsUseCase;
    @Autowired private ReplaceEngineeringChangeStepsUseCase replaceStepsUseCase;
    @Autowired private SubmitEngineeringChangeUseCase submitUseCase;
    @Autowired private ApproveEngineeringChangeReviewUseCase approveReviewUseCase;
    @Autowired private ApproveEngineeringChangeUseCase approveUseCase;
    @Autowired private ReleaseEngineeringChangeUseCase releaseUseCase;
    @Autowired private CancelEngineeringChangeUseCase cancelUseCase;
    @Autowired private RejectEngineeringChangeUseCase rejectUseCase;
    @Autowired private RequestChangesOnStepUseCase requestChangesOnStepUseCase;
    @Autowired private ResubmitStepUseCase resubmitStepUseCase;
    @Autowired private PartRepository partRepository;
    @Autowired private PartRevisionRepository partRevisionRepository;
    @Autowired private EngineeringChangeRepository engineeringChangeRepository;
    @Autowired private EngineeringChangeStepRepository engineeringChangeStepRepository;
    @Autowired private EngineeringChangeAffectedItemRepository affectedItemRepository;
    @Autowired private EngineeringChangeIssueLinkRepository issueLinkRepository;
    @Autowired private EngineeringChangeCommentRepository commentRepository;
    @Autowired private StepStageRepository stepStageRepository;

    private EngineeringChangeIntegrationFixture fixture;
    private User actor;

    @BeforeEach
    void setUp() {
        fixture = new EngineeringChangeIntegrationFixture(
                userRepository, testCurrentAuthProvider, workflowPolicyService,
                createPartUseCase, createPartDraftUseCase, releasePartDraftUseCase,
                changePartLifecycleStateUseCase, updateWorkflowPolicyUseCase,
                createEcUseCase, syncAffectedItemsUseCase, replaceStepsUseCase,
                submitUseCase, approveReviewUseCase, approveUseCase, releaseUseCase, cancelUseCase,
                rejectUseCase, requestChangesOnStepUseCase, resubmitStepUseCase,
                engineeringChangeRepository, engineeringChangeStepRepository,
                affectedItemRepository, issueLinkRepository, commentRepository, stepStageRepository
        );
        fixture.cleanupAll();
        fixture.ensureDefaultPolicy();
        actor = fixture.createUser();
        fixture.setAuth(actor);
    }

    // === EC 모드: 리비전 릴리즈 플로우 ===

    @Test
    void EC모드_리비전_릴리즈_전체_플로우() {
        // given: DIRECT 모드에서 부품 + 릴리즈된 리비전 생성
        fixture.setWorkflowMode(PartRevisionWorkflowMode.DIRECT);
        PartWithReleasedRevision part = fixture.createPartWithReleasedRevision("EC-FLOW-001", "테스트 부품");

        // EC 모드로 전환 후 새 DRAFT 생성
        fixture.setWorkflowMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);
        CreatePartDraftResult draft = fixture.createDraft(part.partId(), part.revisionId());

        // EC 생성 → affected items 등록 → stages 추가
        UUID ecId = fixture.createEc("테스트 EC");
        fixture.syncRevisionReleaseItem(ecId, draft.revisionId());
        fixture.addAllStepsForUser(ecId, actor.getId());

        // when: submit → review → approve → release
        fixture.executeEcReleaseFlow(ecId);

        // then: EC RELEASED
        var releasedEc = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.RELEASED, releasedEc.getState());

        // then: 새 리비전 RELEASED, 이전 리비전 SUPERSEDED
        var releasedRevision = partRevisionRepository.findById(draft.revisionId()).orElseThrow();
        assertEquals(PartRevisionStatus.RELEASED, releasedRevision.getStatus());

        var supersededRevision = partRevisionRepository.findById(part.revisionId()).orElseThrow();
        assertEquals(PartRevisionStatus.SUPERSEDED, supersededRevision.getStatus());
    }

    // === EC 모드: lifecycle 전환 플로우 ===

    @Test
    void EC모드_lifecycle_전환_플로우() {
        // given: EC 모드에서 부품 생성
        fixture.setWorkflowMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);
        var created = fixture.createPart("EC-LIFE-001", "라이프사이클 테스트");

        // EC 생성 → lifecycle 변경 등록 → stages 추가
        UUID ecId = fixture.createEc("EOL 전환");
        fixture.syncLifecycleChangeItem(ecId, created.partId(), PartLifecycleState.EOL);
        fixture.addAllStepsForUser(ecId, actor.getId());

        // when: EC release
        fixture.executeEcReleaseFlow(ecId);

        // then: Part lifecycle이 EOL
        var updated = partRepository.findById(created.partId()).orElseThrow();
        assertEquals(PartLifecycleState.EOL, updated.getLifecycleState());
    }

    // === EC cancel 시 lifecycle 롤백 ===

    @Test
    void EC_cancel시_lifecycle_전환이_롤백된다() {
        // given: EC 모드에서 부품 생성 + lifecycle 변경 등록
        fixture.setWorkflowMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);
        var created = fixture.createPart("EC-CANCEL-001", "취소 테스트");

        UUID ecId = fixture.createEc("취소될 EC");
        fixture.syncLifecycleChangeItem(ecId, created.partId(), PartLifecycleState.OBSOLETE);

        // when: cancel
        fixture.cancelEc(ecId);

        // then: EC CANCELED, Part lifecycle은 여전히 ACTIVE
        var cancelled = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.CANCELED, cancelled.getState());

        var unchanged = partRepository.findById(created.partId()).orElseThrow();
        assertEquals(PartLifecycleState.ACTIVE, unchanged.getLifecycleState());
    }

    // === DIRECT 모드: lifecycle 직접 전환 ===

    @Test
    void DIRECT모드_lifecycle_직접_전환() {
        // given: DIRECT 모드에서 부품 생성
        fixture.setWorkflowMode(PartRevisionWorkflowMode.DIRECT);
        var created = fixture.createPart("DIR-LIFE-001", "직접 전환 테스트");

        // when: ACTIVE → EOL → OBSOLETE
        fixture.changeLifecycleState(created.partId(), PartLifecycleState.EOL);
        var eol = partRepository.findById(created.partId()).orElseThrow();
        assertEquals(PartLifecycleState.EOL, eol.getLifecycleState());

        fixture.changeLifecycleState(created.partId(), PartLifecycleState.OBSOLETE);

        // then
        var obsolete = partRepository.findById(created.partId()).orElseThrow();
        assertEquals(PartLifecycleState.OBSOLETE, obsolete.getLifecycleState());
    }

    // === 예외: EC 모드에서 직접 lifecycle 전환 차단 ===

    @Test
    void EC모드에서_직접_lifecycle_전환은_차단된다() {
        // given
        fixture.setWorkflowMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);
        var created = fixture.createPart("EC-BLOCK-001", "차단 테스트");

        // when & then
        assertThrows(AppException.class, () ->
                fixture.changeLifecycleState(created.partId(), PartLifecycleState.EOL)
        );
    }

    // === 예외: OBSOLETE Part에서 새 DRAFT 생성 차단 ===

    @Test
    void OBSOLETE_부품에서_새_DRAFT_생성이_차단된다() {
        // given: 릴리즈된 부품을 OBSOLETE로 전환
        fixture.setWorkflowMode(PartRevisionWorkflowMode.DIRECT);
        PartWithReleasedRevision part = fixture.createPartWithReleasedRevision("OBS-BLOCK-001", "폐기 차단 테스트");
        fixture.changeLifecycleState(part.partId(), PartLifecycleState.OBSOLETE);

        // when & then: 새 DRAFT 생성 시도 → 차단
        assertThrows(AppException.class, () ->
                fixture.createDraft(part.partId(), part.revisionId())
        );
    }

    // === 예외: 잘못된 lifecycle 전환 ===

    @Test
    void OBSOLETE에서_ACTIVE로_전환하면_예외() {
        // given
        fixture.setWorkflowMode(PartRevisionWorkflowMode.DIRECT);
        var created = fixture.createPart("INV-TRANS-001", "잘못된 전환 테스트");
        fixture.changeLifecycleState(created.partId(), PartLifecycleState.OBSOLETE);

        // when & then
        assertThrows(AppException.class, () ->
                fixture.changeLifecycleState(created.partId(), PartLifecycleState.ACTIVE)
        );
    }
}
