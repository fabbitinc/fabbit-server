package com.fabbitinc.server.integration.engineeringchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeReviewUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CancelEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.RejectEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReleaseEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.AssigneeItem;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.StageItem;
import com.fabbitinc.server.application.engineeringchange.usecase.RequestChangesOnStepUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ResubmitStepUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SubmitEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeAffectedItemsUseCase;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.application.part.usecase.ChangePartLifecycleStateUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.settings.usecase.UpdateSettingsPartWorkflowPolicyUseCase;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.StepStageRepository;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.fixture.EngineeringChangeIntegrationFixture;
import com.fabbitinc.server.integration.support.PostgresIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * EC 승인 워크플로의 Step-Driven 상태 머신 통합 테스트.
 * StepStage 기반의 상태 전이, 완료 정책, 수정 요청/재제출/반려 사이클을 실제 DB에서 검증한다.
 */
class EngineeringChangeStepDrivenStateMachineIntegrationTest extends PostgresIntegrationTestSupport {

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
    @Autowired private EngineeringChangeRepository engineeringChangeRepository;
    @Autowired private EngineeringChangeStepRepository engineeringChangeStepRepository;
    @Autowired private EngineeringChangeAffectedItemRepository affectedItemRepository;
    @Autowired private EngineeringChangeIssueLinkRepository issueLinkRepository;
    @Autowired private EngineeringChangeCommentRepository commentRepository;
    @Autowired private StepStageRepository stepStageRepository;

    private EngineeringChangeIntegrationFixture fixture;
    private User author;

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
        author = fixture.createUser();
        fixture.setAuth(author);
        fixture.setWorkflowMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);
    }

    // === changes_requested 후 resubmit 전체 사이클 ===

    @Test
    void 수정요청_후_재제출하여_승인_완료까지_전체_사이클() {
        // given: EC 생성 + 리뷰어를 별도 사용자로 Stage 구성
        User reviewer = fixture.createUser();
        UUID ecId = fixture.createEc("수정 요청 사이클 테스트");
        fixture.replaceStages(ecId, List.of(
                new StageItem(
                        EngineeringChangeStepType.REVIEW, 1,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer.getId()))
                ),
                new StageItem(
                        EngineeringChangeStepType.APPROVAL, 2,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer.getId()))
                ),
                new StageItem(
                        EngineeringChangeStepType.RELEASE, 3,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer.getId()))
                )
        ));

        // when: submit
        fixture.submitEc(ecId);
        var afterSubmit = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.REVIEW_PENDING, afterSubmit.getState());

        // when: 리뷰어가 수정 요청
        fixture.setAuth(reviewer);
        UUID reviewStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.REVIEW);
        fixture.requestChangesOnStep(ecId, reviewStepId, "설명을 보충해주세요");

        // then: step이 CHANGES_REQUESTED 상태
        var changesRequestedStep = engineeringChangeStepRepository.findById(reviewStepId).orElseThrow();
        assertEquals(EngineeringChangeStepStatus.CHANGES_REQUESTED, changesRequestedStep.getStatus());

        // when: 작성자가 재제출
        fixture.setAuth(author);
        UUID changesRequestedStepId = fixture.findChangesRequestedStepIdByStageType(ecId, EngineeringChangeStepType.REVIEW);
        fixture.resubmitStep(ecId, changesRequestedStepId);

        // then: step이 다시 PENDING
        var resubmittedStep = engineeringChangeStepRepository.findById(reviewStepId).orElseThrow();
        assertEquals(EngineeringChangeStepStatus.PENDING, resubmittedStep.getStatus());

        // when: 리뷰어가 다시 승인 → approval 승인 → release 승인
        fixture.setAuth(reviewer);
        UUID reviewStepId2 = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.REVIEW);
        fixture.approveReviewStep(ecId, reviewStepId2);

        var afterReviewApprove = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, afterReviewApprove.getState());

        UUID approvalStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.APPROVAL);
        fixture.approveStep(ecId, approvalStepId);

        var afterApproval = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.RELEASE_PENDING, afterApproval.getState());

        UUID releaseStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.RELEASE);
        fixture.releaseStep(ecId, releaseStepId);

        // then: EC RELEASED
        var releasedEc = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.RELEASED, releasedEc.getState());
    }

    // === reject 후 전체 리셋 ===

    @Test
    void 반려_후_EC가_DRAFT로_복귀하고_모든_step이_PENDING으로_리셋된다() {
        // given: EC 생성 + Stage 구성
        User reviewer = fixture.createUser();
        UUID ecId = fixture.createEc("반려 리셋 테스트");
        fixture.replaceStages(ecId, List.of(
                new StageItem(
                        EngineeringChangeStepType.REVIEW, 1,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer.getId()))
                ),
                new StageItem(
                        EngineeringChangeStepType.APPROVAL, 2,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer.getId()))
                ),
                new StageItem(
                        EngineeringChangeStepType.RELEASE, 3,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer.getId()))
                )
        ));

        // when: submit
        fixture.submitEc(ecId);
        var afterSubmit = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.REVIEW_PENDING, afterSubmit.getState());

        // when: 리뷰어가 반려
        fixture.setAuth(reviewer);
        UUID reviewStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.REVIEW);
        fixture.rejectStep(ecId, reviewStepId, "설계 방향이 맞지 않습니다");

        // then: EC가 DRAFT로 복귀
        var rejectedEc = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.DRAFT, rejectedEc.getState());

        // then: 모든 step이 PENDING으로 리셋
        var allStatuses = fixture.getAllStepStatuses(ecId);
        assertTrue(allStatuses.stream().allMatch(s -> s == EngineeringChangeStepStatus.PENDING),
                "반려 후 모든 step이 PENDING 상태여야 합니다: " + allStatuses);
    }

    // === ANY_ONE_APPROVES 정책 ===

    @Test
    void ANY_ONE_APPROVES_정책에서_1명_승인시_다음_단계로_전이된다() {
        // given: 2명의 리뷰어로 REVIEW Stage를 ANY_ONE_APPROVES로 구성
        User reviewer1 = fixture.createUser();
        User reviewer2 = fixture.createUser();
        User approver = fixture.createUser();
        UUID ecId = fixture.createEc("ANY_ONE 정책 테스트");
        fixture.replaceStages(ecId, List.of(
                new StageItem(
                        EngineeringChangeStepType.REVIEW, 1,
                        StepStageCompletionPolicy.ANY_ONE_APPROVES, null, null,
                        List.of(
                                new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer1.getId()),
                                new AssigneeItem(EngineeringChangeStepAssigneeType.USER, reviewer2.getId())
                        )
                ),
                new StageItem(
                        EngineeringChangeStepType.APPROVAL, 2,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, approver.getId()))
                ),
                new StageItem(
                        EngineeringChangeStepType.RELEASE, 3,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                        List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, approver.getId()))
                )
        ));

        // when: submit
        fixture.submitEc(ecId);
        var afterSubmit = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.REVIEW_PENDING, afterSubmit.getState());

        // when: reviewer1만 승인 (reviewer2는 승인하지 않음)
        fixture.setAuth(reviewer1);
        UUID reviewer1StepId = findPendingStepIdForUser(ecId, EngineeringChangeStepType.REVIEW, reviewer1.getId());
        fixture.approveReviewStep(ecId, reviewer1StepId);

        // then: ANY_ONE_APPROVES이므로 APPROVAL_PENDING으로 전이
        var afterOneApproval = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, afterOneApproval.getState());

        // then: reviewer2의 step은 CANCELED 상태
        var allSteps = engineeringChangeStepRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(ecId);
        var reviewer2Step = allSteps.stream()
                .filter(s -> s.getAssigneeId().equals(reviewer2.getId()))
                .findFirst().orElseThrow();
        assertEquals(EngineeringChangeStepStatus.CANCELED, reviewer2Step.getStatus());

        // when: approver가 승인 → release
        fixture.setAuth(approver);
        UUID approvalStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.APPROVAL);
        fixture.approveStep(ecId, approvalStepId);

        UUID releaseStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.RELEASE);
        fixture.releaseStep(ecId, releaseStepId);

        // then: EC RELEASED
        var releasedEc = engineeringChangeRepository.findById(ecId).orElseThrow();
        assertEquals(EngineeringChangeState.RELEASED, releasedEc.getState());
    }

    // === 헬퍼 메서드 ===

    /**
     * 특정 사용자에게 할당된 PENDING step을 찾는다.
     */
    private UUID findPendingStepIdForUser(UUID ecId, EngineeringChangeStepType stepType, UUID userId) {
        var stages = stepStageRepository.findByEngineeringChangeIdOrderBySequenceAsc(ecId);
        var targetStage = stages.stream()
                .filter(stage -> stage.getStepType() == stepType)
                .findFirst()
                .orElseThrow();

        var steps = engineeringChangeStepRepository
                .findByStepStageIdAndStatusOrderByCreatedAtAsc(targetStage.getId(), EngineeringChangeStepStatus.PENDING);

        return steps.stream()
                .filter(step -> step.getAssigneeId().equals(userId))
                .findFirst()
                .map(EngineeringChangeStep::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "EC " + ecId + "에서 사용자 " + userId + "에게 할당된 " + stepType + " PENDING step을 찾을 수 없습니다"));
    }
}
