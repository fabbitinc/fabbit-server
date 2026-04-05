package com.fabbitinc.server.integration.fixture;

import com.fabbitinc.server.application.auth.support.AuthContext;
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
import com.fabbitinc.server.application.part.usecase.command.CreatePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.application.part.usecase.result.CreatePartResult;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartDraftResult;
import com.fabbitinc.server.application.settings.usecase.UpdateSettingsPartWorkflowPolicyUseCase;
import com.fabbitinc.server.application.settings.usecase.command.UpdateSettingsPartWorkflowPolicyCommand;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStage;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.StepStageRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.support.TestCurrentAuthProvider;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class EngineeringChangeIntegrationFixture {

    private final UserRepository userRepository;
    private final TestCurrentAuthProvider testCurrentAuthProvider;
    private final PartRevisionWorkflowPolicyService workflowPolicyService;
    private final CreatePartUseCase createPartUseCase;
    private final CreatePartDraftUseCase createPartDraftUseCase;
    private final ReleasePartDraftUseCase releasePartDraftUseCase;
    private final ChangePartLifecycleStateUseCase changePartLifecycleStateUseCase;
    private final UpdateSettingsPartWorkflowPolicyUseCase updateWorkflowPolicyUseCase;
    private final CreateEngineeringChangeUseCase createEcUseCase;
    private final SyncEngineeringChangeAffectedItemsUseCase syncAffectedItemsUseCase;
    private final ReplaceEngineeringChangeStepsUseCase replaceStepsUseCase;
    private final SubmitEngineeringChangeUseCase submitUseCase;
    private final ApproveEngineeringChangeReviewUseCase approveReviewUseCase;
    private final ApproveEngineeringChangeUseCase approveUseCase;
    private final ReleaseEngineeringChangeUseCase releaseUseCase;
    private final CancelEngineeringChangeUseCase cancelUseCase;
    private final RejectEngineeringChangeUseCase rejectUseCase;
    private final RequestChangesOnStepUseCase requestChangesOnStepUseCase;
    private final ResubmitStepUseCase resubmitStepUseCase;
    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeStepRepository engineeringChangeStepRepository;
    private final EngineeringChangeAffectedItemRepository affectedItemRepository;
    private final EngineeringChangeIssueLinkRepository issueLinkRepository;
    private final EngineeringChangeCommentRepository commentRepository;
    private final StepStageRepository stepStageRepository;

    public EngineeringChangeIntegrationFixture(
            UserRepository userRepository,
            TestCurrentAuthProvider testCurrentAuthProvider,
            PartRevisionWorkflowPolicyService workflowPolicyService,
            CreatePartUseCase createPartUseCase,
            CreatePartDraftUseCase createPartDraftUseCase,
            ReleasePartDraftUseCase releasePartDraftUseCase,
            ChangePartLifecycleStateUseCase changePartLifecycleStateUseCase,
            UpdateSettingsPartWorkflowPolicyUseCase updateWorkflowPolicyUseCase,
            CreateEngineeringChangeUseCase createEcUseCase,
            SyncEngineeringChangeAffectedItemsUseCase syncAffectedItemsUseCase,
            ReplaceEngineeringChangeStepsUseCase replaceStepsUseCase,
            SubmitEngineeringChangeUseCase submitUseCase,
            ApproveEngineeringChangeReviewUseCase approveReviewUseCase,
            ApproveEngineeringChangeUseCase approveUseCase,
            ReleaseEngineeringChangeUseCase releaseUseCase,
            CancelEngineeringChangeUseCase cancelUseCase,
            RejectEngineeringChangeUseCase rejectUseCase,
            RequestChangesOnStepUseCase requestChangesOnStepUseCase,
            ResubmitStepUseCase resubmitStepUseCase,
            EngineeringChangeRepository engineeringChangeRepository,
            EngineeringChangeStepRepository engineeringChangeStepRepository,
            EngineeringChangeAffectedItemRepository affectedItemRepository,
            EngineeringChangeIssueLinkRepository issueLinkRepository,
            EngineeringChangeCommentRepository commentRepository,
            StepStageRepository stepStageRepository
    ) {
        this.userRepository = userRepository;
        this.testCurrentAuthProvider = testCurrentAuthProvider;
        this.workflowPolicyService = workflowPolicyService;
        this.createPartUseCase = createPartUseCase;
        this.createPartDraftUseCase = createPartDraftUseCase;
        this.releasePartDraftUseCase = releasePartDraftUseCase;
        this.changePartLifecycleStateUseCase = changePartLifecycleStateUseCase;
        this.updateWorkflowPolicyUseCase = updateWorkflowPolicyUseCase;
        this.createEcUseCase = createEcUseCase;
        this.syncAffectedItemsUseCase = syncAffectedItemsUseCase;
        this.replaceStepsUseCase = replaceStepsUseCase;
        this.submitUseCase = submitUseCase;
        this.approveReviewUseCase = approveReviewUseCase;
        this.approveUseCase = approveUseCase;
        this.releaseUseCase = releaseUseCase;
        this.cancelUseCase = cancelUseCase;
        this.rejectUseCase = rejectUseCase;
        this.requestChangesOnStepUseCase = requestChangesOnStepUseCase;
        this.resubmitStepUseCase = resubmitStepUseCase;
        this.engineeringChangeRepository = engineeringChangeRepository;
        this.engineeringChangeStepRepository = engineeringChangeStepRepository;
        this.affectedItemRepository = affectedItemRepository;
        this.issueLinkRepository = issueLinkRepository;
        this.commentRepository = commentRepository;
        this.stepStageRepository = stepStageRepository;
    }

    // === 컨텍스트 설정 ===

    public User createUser() {
        String email = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        return userRepository.save(User.create(email, "hashed", "테스트 사용자"));
    }

    public void setAuth(User user) {
        testCurrentAuthProvider.set(new AuthContext(
                user.getId(), user.getEmail(), UUID.randomUUID(), MembershipRole.OWNER
        ));
    }

    public void setWorkflowMode(PartRevisionWorkflowMode mode) {
        updateWorkflowPolicyUseCase.execute(new UpdateSettingsPartWorkflowPolicyCommand(mode));
    }

    public void ensureDefaultPolicy() {
        workflowPolicyService.ensureDefaultPolicyExists();
    }

    // === Part 생성 ===

    public CreatePartResult createPart(String partNumber, String name) {
        return createPartUseCase.execute(new com.fabbitinc.server.application.part.usecase.command.CreatePartCommand(
                partNumber, null, null, name, null, null, null, null, null, null, null
        ));
    }

    public PartWithReleasedRevision createPartWithReleasedRevision(String partNumber, String name) {
        CreatePartResult created = createPart(partNumber, name);
        ReleasePartDraftResult released = releasePartDraftUseCase.execute(
                new ReleasePartDraftCommand(created.partId(), created.revisionId(), "초기 릴리즈")
        );
        return new PartWithReleasedRevision(released.partId(), released.revisionId());
    }

    public CreatePartDraftResult createDraft(UUID partId, UUID baseRevisionId) {
        return createPartDraftUseCase.execute(
                new CreatePartDraftCommand(partId, baseRevisionId, "새 초안 생성")
        );
    }

    // === Lifecycle 전환 ===

    public void changeLifecycleState(UUID partId, PartLifecycleState targetState) {
        changePartLifecycleStateUseCase.execute(
                new ChangePartLifecycleStateUseCase.ChangePartLifecycleStateCommand(partId, targetState)
        );
    }

    // === EC 생성 및 흐름 ===

    public UUID createEc(String title) {
        return createEcUseCase.execute(new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand(
                title, null, null, List.of(), List.of(), List.of()
        )).engineeringChangeId();
    }

    public void syncRevisionReleaseItem(UUID ecId, UUID revisionId) {
        syncAffectedItemsUseCase.execute(
                new SyncEngineeringChangeAffectedItemsUseCase.SyncEngineeringChangeAffectedItemsCommand(
                        ecId,
                        List.of(new SyncEngineeringChangeAffectedItemsUseCase.Item(
                                EngineeringChangeAffectedItemType.REVISION_RELEASE, revisionId, null
                        ))
                )
        );
    }

    public void syncLifecycleChangeItem(UUID ecId, UUID partId, PartLifecycleState targetState) {
        syncAffectedItemsUseCase.execute(
                new SyncEngineeringChangeAffectedItemsUseCase.SyncEngineeringChangeAffectedItemsCommand(
                        ecId,
                        List.of(new SyncEngineeringChangeAffectedItemsUseCase.Item(
                                EngineeringChangeAffectedItemType.LIFECYCLE_CHANGE, partId, targetState
                        ))
                )
        );
    }

    /**
     * EC에 3개 Stage(REVIEW, APPROVAL, RELEASE)를 ALL_MUST_APPROVE 정책으로 추가한다.
     * 각 Stage에 지정된 userId를 담당자로 할당한다.
     */
    public void addAllStagesForUser(UUID ecId, UUID userId) {
        replaceStepsUseCase.execute(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand(
                ecId,
                List.of(
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.StageItem(
                                EngineeringChangeStepType.REVIEW, 1,
                                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                                List.of(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.AssigneeItem(
                                        EngineeringChangeStepAssigneeType.USER, userId
                                ))
                        ),
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.StageItem(
                                EngineeringChangeStepType.APPROVAL, 2,
                                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                                List.of(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.AssigneeItem(
                                        EngineeringChangeStepAssigneeType.USER, userId
                                ))
                        ),
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.StageItem(
                                EngineeringChangeStepType.RELEASE, 3,
                                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                                List.of(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.AssigneeItem(
                                        EngineeringChangeStepAssigneeType.USER, userId
                                ))
                        )
                )
        ));
    }

    /**
     * 이전 호환성을 위해 addAllStepsForUser를 addAllStagesForUser로 위임한다.
     */
    public void addAllStepsForUser(UUID ecId, UUID userId) {
        addAllStagesForUser(ecId, userId);
    }

    /**
     * EC에 Stage를 직접 구성한다. 완료 정책과 담당자를 세밀하게 제어할 때 사용한다.
     */
    public void replaceStages(UUID ecId, List<ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.StageItem> stages) {
        replaceStepsUseCase.execute(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand(
                ecId, stages
        ));
    }

    /**
     * submit → review approve → approval approve → release approve 전체 플로우를 실행한다.
     * 각 step approve 후 EC를 다시 로드하여 최신 상태를 반영한다.
     */
    public void executeEcReleaseFlow(UUID ecId) {
        // submit
        submitUseCase.execute(new SubmitEngineeringChangeUseCase.SubmitEngineeringChangeCommand(ecId));

        // review step 승인
        UUID reviewStepId = findPendingStepIdByStageType(ecId, EngineeringChangeStepType.REVIEW);
        approveReviewUseCase.execute(
                new ApproveEngineeringChangeReviewUseCase.ApproveEngineeringChangeReviewCommand(ecId, reviewStepId)
        );

        // approval step 승인
        UUID approvalStepId = findPendingStepIdByStageType(ecId, EngineeringChangeStepType.APPROVAL);
        approveUseCase.execute(
                new ApproveEngineeringChangeUseCase.ApproveEngineeringChangeCommand(ecId, approvalStepId)
        );

        // release step 승인 (ReleaseUseCase가 approve 후 자동 release 처리)
        UUID releaseStepId = findPendingStepIdByStageType(ecId, EngineeringChangeStepType.RELEASE);
        releaseUseCase.execute(
                new ReleaseEngineeringChangeUseCase.ReleaseEngineeringChangeCommand(ecId, releaseStepId)
        );
    }

    // === Step 액션 ===

    public void submitEc(UUID ecId) {
        submitUseCase.execute(new SubmitEngineeringChangeUseCase.SubmitEngineeringChangeCommand(ecId));
    }

    public void approveReviewStep(UUID ecId, UUID stepId) {
        approveReviewUseCase.execute(
                new ApproveEngineeringChangeReviewUseCase.ApproveEngineeringChangeReviewCommand(ecId, stepId)
        );
    }

    public void approveStep(UUID ecId, UUID stepId) {
        approveUseCase.execute(
                new ApproveEngineeringChangeUseCase.ApproveEngineeringChangeCommand(ecId, stepId)
        );
    }

    public void releaseStep(UUID ecId, UUID stepId) {
        releaseUseCase.execute(
                new ReleaseEngineeringChangeUseCase.ReleaseEngineeringChangeCommand(ecId, stepId)
        );
    }

    public void rejectStep(UUID ecId, UUID stepId, String comment) {
        rejectUseCase.execute(
                new RejectEngineeringChangeUseCase.RejectEngineeringChangeCommand(ecId, stepId, comment)
        );
    }

    public void requestChangesOnStep(UUID ecId, UUID stepId, String comment) {
        requestChangesOnStepUseCase.execute(
                new RequestChangesOnStepUseCase.RequestChangesOnStepCommand(ecId, stepId, comment)
        );
    }

    public void resubmitStep(UUID ecId, UUID stepId) {
        resubmitStepUseCase.execute(
                new ResubmitStepUseCase.ResubmitStepCommand(ecId, stepId)
        );
    }

    public void cancelEc(UUID ecId) {
        cancelUseCase.execute(new CancelEngineeringChangeUseCase.CancelEngineeringChangeCommand(ecId));
    }

    // === 정리 ===

    /**
     * 모든 EC 관련 데이터를 삭제한다. 테스트 간 격리를 위해 사용한다.
     */
    public void cleanupAll() {
        engineeringChangeStepRepository.deleteAll();
        stepStageRepository.deleteAll();
        affectedItemRepository.deleteAll();
        issueLinkRepository.deleteAll();
        commentRepository.deleteAll();
        engineeringChangeRepository.deleteAll();
    }

    // === Step 조회 헬퍼 ===

    /**
     * EC의 특정 StepType Stage에 속한 PENDING 상태의 step ID를 찾는다.
     */
    public UUID findPendingStepIdByStageType(UUID ecId, EngineeringChangeStepType stepType) {
        List<StepStage> stages = stepStageRepository.findByEngineeringChangeIdOrderBySequenceAsc(ecId);

        StepStage targetStage = stages.stream()
                .filter(stage -> stage.getStepType() == stepType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "EC " + ecId + "에서 " + stepType + " 타입의 Stage를 찾을 수 없습니다"));

        List<EngineeringChangeStep> steps = engineeringChangeStepRepository
                .findByStepStageIdAndStatusOrderByCreatedAtAsc(targetStage.getId(), EngineeringChangeStepStatus.PENDING);

        return steps.stream()
                .findFirst()
                .map(EngineeringChangeStep::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "EC " + ecId + "에서 " + stepType + " Stage의 PENDING step을 찾을 수 없습니다"));
    }

    /**
     * EC의 특정 StepType Stage에 속한 CHANGES_REQUESTED 상태의 step ID를 찾는다.
     */
    public UUID findChangesRequestedStepIdByStageType(UUID ecId, EngineeringChangeStepType stepType) {
        List<StepStage> stages = stepStageRepository.findByEngineeringChangeIdOrderBySequenceAsc(ecId);

        StepStage targetStage = stages.stream()
                .filter(stage -> stage.getStepType() == stepType)
                .findFirst()
                .orElseThrow();

        List<EngineeringChangeStep> steps = engineeringChangeStepRepository
                .findByStepStageIdAndStatusOrderByCreatedAtAsc(targetStage.getId(), EngineeringChangeStepStatus.CHANGES_REQUESTED);

        return steps.stream()
                .findFirst()
                .map(EngineeringChangeStep::getId)
                .orElseThrow();
    }

    /**
     * EC의 모든 step 상태 목록을 반환한다.
     */
    public List<EngineeringChangeStepStatus> getAllStepStatuses(UUID ecId) {
        return engineeringChangeStepRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(ecId).stream()
                .map(EngineeringChangeStep::getStatus)
                .toList();
    }

    // === Result records ===

    public record PartWithReleasedRevision(UUID partId, UUID revisionId) {
    }
}
