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
import com.fabbitinc.server.application.settings.usecase.UpdateSettingsPartWorkflowPolicyUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncAssigneesUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import com.fabbitinc.server.domain.notification.repository.NotificationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.StepStageRepository;
import com.fabbitinc.server.integration.fixture.EngineeringChangeIntegrationFixture;
import com.fabbitinc.server.integration.fixture.EngineeringChangeIntegrationFixture.PartWithReleasedRevision;
import com.fabbitinc.server.integration.support.PostgresIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EngineeringChangeReleaseNotificationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired private UserRepository userRepository;
    @Autowired private CreatePartUseCase createPartUseCase;
    @Autowired private CreatePartDraftUseCase createPartDraftUseCase;
    @Autowired private ReleasePartDraftUseCase releasePartDraftUseCase;
    @Autowired private CreateEngineeringChangeUseCase createEngineeringChangeUseCase;
    @Autowired private SyncEngineeringChangeAffectedItemsUseCase syncAffectedItemsUseCase;
    @Autowired private ReplaceEngineeringChangeStepsUseCase replaceStepsUseCase;
    @Autowired private SubmitEngineeringChangeUseCase submitUseCase;
    @Autowired private ReleaseEngineeringChangeUseCase releaseUseCase;
    @Autowired private ApproveEngineeringChangeReviewUseCase approveReviewUseCase;
    @Autowired private ApproveEngineeringChangeUseCase approveUseCase;
    @Autowired private CancelEngineeringChangeUseCase cancelUseCase;
    @Autowired private RejectEngineeringChangeUseCase rejectUseCase;
    @Autowired private RequestChangesOnStepUseCase requestChangesOnStepUseCase;
    @Autowired private ResubmitStepUseCase resubmitStepUseCase;
    @Autowired private PartRevisionWorkflowPolicyService workflowPolicyService;
    @Autowired private UpdateSettingsPartWorkflowPolicyUseCase updateWorkflowPolicyUseCase;
    @Autowired private CreateIssueUseCase createIssueUseCase;
    @Autowired private SyncAssigneesUseCase syncAssigneesUseCase;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EngineeringChangeRepository engineeringChangeRepository;
    @Autowired private EngineeringChangeStepRepository engineeringChangeStepRepository;
    @Autowired private EngineeringChangeAffectedItemRepository affectedItemRepository;
    @Autowired private EngineeringChangeIssueLinkRepository issueLinkRepository;
    @Autowired private EngineeringChangeCommentRepository commentRepository;
    @Autowired private StepStageRepository stepStageRepository;

    private EngineeringChangeIntegrationFixture fixture;
    private User actor;
    private User reviewer;

    @BeforeEach
    void setUp() {
        fixture = new EngineeringChangeIntegrationFixture(
                userRepository, testCurrentAuthProvider, workflowPolicyService,
                createPartUseCase, createPartDraftUseCase, releasePartDraftUseCase,
                null, updateWorkflowPolicyUseCase,
                createEngineeringChangeUseCase, syncAffectedItemsUseCase, replaceStepsUseCase,
                submitUseCase, approveReviewUseCase, approveUseCase, releaseUseCase, cancelUseCase,
                rejectUseCase, requestChangesOnStepUseCase, resubmitStepUseCase,
                engineeringChangeRepository, engineeringChangeStepRepository,
                affectedItemRepository, issueLinkRepository, commentRepository, stepStageRepository
        );
        fixture.cleanupAll();
        fixture.ensureDefaultPolicy();
        actor = fixture.createUser();
        reviewer = fixture.createUser();
        fixture.setAuth(actor);
        fixture.setWorkflowMode(PartRevisionWorkflowMode.DIRECT);
    }

    @Test
    void EC_릴리즈시_연결이슈_담당자에게_RELEASE_알림이_생성된다() {
        // given: DIRECT 모드에서 부품 + 릴리즈된 리비전 생성
        PartWithReleasedRevision part = fixture.createPartWithReleasedRevision("NOTI-001", "알림 테스트 부품");

        // EC 모드로 전환 후 새 DRAFT 생성
        fixture.setWorkflowMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);
        CreatePartDraftResult draft = fixture.createDraft(part.partId(), part.revisionId());

        UUID issueId = createIssueUseCase.execute(new CreateIssueUseCase.CreateIssueCommand(
                "연결 이슈", null, List.of(part.partId()), List.of(), List.of(), List.of(), List.of()
        )).issueId();
        syncAssigneesUseCase.execute(new SyncAssigneesUseCase.SyncAssigneesCommand(issueId, List.of(reviewer.getId())));

        UUID ecId = createEngineeringChangeUseCase.execute(new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand(
                "릴리즈 알림 EC", null, issueId, List.of(), List.of(), List.of()
        )).engineeringChangeId();
        syncAffectedItemsUseCase.execute(new SyncEngineeringChangeAffectedItemsUseCase.SyncEngineeringChangeAffectedItemsCommand(
                ecId,
                List.of(new SyncEngineeringChangeAffectedItemsUseCase.Item(EngineeringChangeAffectedItemType.REVISION_RELEASE, draft.revisionId(), null))
        ));
        replaceStepsUseCase.execute(new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand(
                ecId,
                List.of(
                        new StageItem(
                                EngineeringChangeStepType.RELEASE, 1,
                                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null,
                                List.of(new AssigneeItem(EngineeringChangeStepAssigneeType.USER, actor.getId()))
                        )
                )
        ));
        submitUseCase.execute(new SubmitEngineeringChangeUseCase.SubmitEngineeringChangeCommand(ecId));

        // when
        UUID releaseStepId = fixture.findPendingStepIdByStageType(ecId, EngineeringChangeStepType.RELEASE);
        releaseUseCase.execute(new ReleaseEngineeringChangeUseCase.ReleaseEngineeringChangeCommand(ecId, releaseStepId));

        // then
        var notifications = notificationRepository.findAll().stream()
                .filter(notification -> reviewer.getId().equals(notification.getUserId()))
                .toList();
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.RELEASE, notifications.getFirst().getType());
        assertTrue(notifications.getFirst().getPayload().contains("릴리즈 알림 EC"));
    }
}
