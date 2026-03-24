package com.fabbitinc.server.integration.fixture;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeReviewUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CancelEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReleaseEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SubmitEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeAffectedItemsUseCase;
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
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.integration.support.TestCurrentAuthProvider;
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
            CancelEngineeringChangeUseCase cancelUseCase
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
                partNumber, name, null, null, null, null, null, null, null, null, null
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

    public void addAllStepsForUser(UUID ecId, UUID userId) {
        replaceStepsUseCase.execute(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand(
                ecId,
                List.of(
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.Item(
                                EngineeringChangeStepType.REVIEW, EngineeringChangeStepAssigneeType.USER, userId, 1
                        ),
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.Item(
                                EngineeringChangeStepType.APPROVAL, EngineeringChangeStepAssigneeType.USER, userId, 2
                        ),
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.Item(
                                EngineeringChangeStepType.RELEASE, EngineeringChangeStepAssigneeType.USER, userId, 3
                        )
                )
        ));
    }

    public void executeEcReleaseFlow(UUID ecId) {
        submitUseCase.execute(new SubmitEngineeringChangeUseCase.SubmitEngineeringChangeCommand(ecId));
        approveReviewUseCase.execute(new ApproveEngineeringChangeReviewUseCase.ApproveEngineeringChangeReviewCommand(ecId));
        approveUseCase.execute(new ApproveEngineeringChangeUseCase.ApproveEngineeringChangeCommand(ecId));
        releaseUseCase.execute(new ReleaseEngineeringChangeUseCase.ReleaseEngineeringChangeCommand(ecId));
    }

    public void cancelEc(UUID ecId) {
        cancelUseCase.execute(new CancelEngineeringChangeUseCase.CancelEngineeringChangeCommand(ecId));
    }

    // === Result records ===

    public record PartWithReleasedRevision(UUID partId, UUID revisionId) {
    }
}
