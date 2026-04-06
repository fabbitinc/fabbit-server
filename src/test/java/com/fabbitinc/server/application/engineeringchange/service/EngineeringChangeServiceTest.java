package com.fabbitinc.server.application.engineeringchange.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.workitem.support.MentionExtractor;
import com.fabbitinc.server.application.workitem.support.TipTapValidator;
import com.fabbitinc.server.domain.activity.repository.ActivityRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStep;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepStatus;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.engineeringchange.model.StepStage;
import com.fabbitinc.server.domain.engineeringchange.model.StepStageCompletionPolicy;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.StepStageRepository;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.domain.workitem.repository.WorkItemNumberSequenceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EngineeringChangeServiceTest {

    @Mock
    private IssueApi issueApi;
    @Mock
    private WorkItemNumberSequenceRepository workItemNumberSequenceRepository;
    @Mock
    private EngineeringChangeRepository engineeringChangeRepository;
    @Mock
    private EngineeringChangeStepRepository engineeringChangeStepRepository;
    @Mock
    private EngineeringChangeIssueLinkRepository engineeringChangeIssueRepository;
    @Mock
    private EngineeringChangeCommentRepository engineeringChangeCommentRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private OrganizationApi organizationApi;
    @Mock
    private TipTapValidator tipTapValidator;
    @Mock
    private MentionExtractor mentionExtractor;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StepStageRepository stepStageRepository;

    private StepCompletionEvaluator stepCompletionEvaluator;

    private EngineeringChangeService engineeringChangeService;

    @BeforeEach
    void setUp() {
        stepCompletionEvaluator = new StepCompletionEvaluator();

        engineeringChangeService = new EngineeringChangeService(
                issueApi,
                workItemNumberSequenceRepository,
                engineeringChangeRepository,
                engineeringChangeStepRepository,
                engineeringChangeIssueRepository,
                engineeringChangeCommentRepository,
                teamMemberRepository,
                teamRepository,
                userRepository,
                fileRepository,
                activityRepository,
                applicationEventPublisher,
                organizationApi,
                tipTapValidator,
                mentionExtractor,
                objectMapper,
                stepCompletionEvaluator,
                stepStageRepository
        );

        lenient().when(engineeringChangeRepository.existsById(any())).thenReturn(true);
        lenient().when(issueApi.existsIssue(any())).thenReturn(false);
    }

    @Test
    void approveStep_다중ReviewStep이면_모두승인되기전까지ReviewPending을유지한다() {
        UUID actorId = UUID.randomUUID();
        UUID firstReviewerId = UUID.randomUUID();
        UUID secondReviewerId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(101, "변경", "본문", null, actorId);

        // Stage 생성: REVIEW, sequence=1, ALL_MUST_APPROVE
        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep firstStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, firstReviewerId, actorId);
        EngineeringChangeStep secondStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, secondReviewerId, actorId);

        // APPROVAL stage 추가 (REVIEW 다음 단계)
        StepStage approvalStage = ec.addStage(
                EngineeringChangeStepType.APPROVAL, 2,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        ec.addStep(approvalStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        ec.submit(actorId);

        when(stepStageRepository.findById(reviewStage.getId())).thenReturn(Optional.of(reviewStage));
        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of());

        // 첫 번째 리뷰어 승인 → ALL_MUST_APPROVE이므로 아직 stage 미완료
        engineeringChangeService.approveStep(firstReviewerId, ec, firstStep.getId());

        assertEquals(EngineeringChangeStepStatus.APPROVED, firstStep.getStatus());
        assertEquals(EngineeringChangeStepStatus.PENDING, secondStep.getStatus());
        assertEquals(EngineeringChangeState.REVIEW_PENDING, ec.getState());

        // 두 번째 리뷰어 승인 → stage 완료 → APPROVAL_PENDING으로 전이
        engineeringChangeService.approveStep(secondReviewerId, ec, secondStep.getId());

        assertEquals(EngineeringChangeStepStatus.APPROVED, secondStep.getStatus());
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, ec.getState());
    }

    @Test
    void approveStep_팀ReviewStep이면_팀원사용자가승인할수있다() {
        UUID actorId = UUID.randomUUID();
        Team team = Team.create("검토팀", null, actorId);
        TeamMember teamMember = team.addMember(actorId);
        EngineeringChange ec = EngineeringChange.create(102, "변경", "본문", null, actorId);

        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep reviewStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.TEAM, team.getId(), actorId);

        // APPROVAL stage 추가 (REVIEW 다음 단계)
        StepStage approvalStage = ec.addStage(
                EngineeringChangeStepType.APPROVAL, 2,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        ec.addStep(approvalStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        ec.submit(actorId);

        when(stepStageRepository.findById(reviewStage.getId())).thenReturn(Optional.of(reviewStage));
        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of(teamMember));

        engineeringChangeService.approveStep(actorId, ec, reviewStep.getId());

        assertEquals(EngineeringChangeStepStatus.APPROVED, reviewStep.getStatus());
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, ec.getState());
    }

    @Test
    void rejectStep_approvalPending이면Draft로되돌아간다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(103, "변경", "본문", null, actorId);

        // REVIEW stage + step (승인 처리 후 submit)
        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep reviewStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);
        reviewStep.approve(actorId, java.time.Instant.now());

        // APPROVAL stage + step
        StepStage approvalStage = ec.addStage(
                EngineeringChangeStepType.APPROVAL, 2,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep approvalStep = ec.addStep(
                approvalStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        ec.submit(actorId);
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, ec.getState());

        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of());

        engineeringChangeService.rejectStep(actorId, ec, approvalStep.getId(), null);

        assertEquals(EngineeringChangeStepStatus.PENDING, approvalStep.getStatus()); // reset 이후 PENDING
        assertEquals(EngineeringChangeState.DRAFT, ec.getState());
    }

    @Test
    void rejectStep_releasePending이면Draft로되돌아간다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(104, "변경", "본문", null, actorId);

        // REVIEW stage + step (사전 승인)
        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep reviewStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);
        reviewStep.approve(actorId, java.time.Instant.now());

        // APPROVAL stage + step (사전 승인)
        StepStage approvalStage = ec.addStage(
                EngineeringChangeStepType.APPROVAL, 2,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep approvalStep = ec.addStep(
                approvalStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);
        approvalStep.approve(actorId, java.time.Instant.now());

        // RELEASE stage + step
        StepStage releaseStage = ec.addStage(
                EngineeringChangeStepType.RELEASE, 3,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep releaseStep = ec.addStep(
                releaseStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        ec.submit(actorId);
        assertEquals(EngineeringChangeState.RELEASE_PENDING, ec.getState());

        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of());

        engineeringChangeService.rejectStep(actorId, ec, releaseStep.getId(), null);

        assertEquals(EngineeringChangeStepStatus.PENDING, releaseStep.getStatus()); // reset 이후 PENDING
        assertEquals(EngineeringChangeState.DRAFT, ec.getState());
    }

    @Test
    void syncStages_기존Step을먼저지우고_그다음Stage를지운다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(200, "변경", "본문", null, actorId);

        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep reviewStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        engineeringChangeService.syncStages(actorId, ec, List.of());

        InOrder inOrder = inOrder(engineeringChangeStepRepository, stepStageRepository);
        inOrder.verify(engineeringChangeStepRepository).deleteAll(List.of(reviewStep));
        inOrder.verify(engineeringChangeStepRepository).flush();
        inOrder.verify(stepStageRepository).deleteAll(List.of(reviewStage));
        inOrder.verify(stepStageRepository).flush();
        assertEquals(0, ec.getSteps().size());
        assertEquals(0, ec.getStages().size());
    }

    @Test
    void syncStages_기존Stage는유지하고_담당자만차등반영한다() {
        UUID actorId = UUID.randomUUID();
        UUID secondReviewerId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(201, "변경", "본문", null, actorId);
        User actor = org.mockito.Mockito.mock(User.class);
        User secondReviewer = org.mockito.Mockito.mock(User.class);

        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        EngineeringChangeStep existingStep = ec.addStep(
                reviewStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userRepository.findById(secondReviewerId)).thenReturn(Optional.of(secondReviewer));

        engineeringChangeService.syncStages(
                actorId,
                ec,
                List.of(new EngineeringChangeService.StageDraft(
                        reviewStage.getId(),
                        EngineeringChangeStepType.REVIEW,
                        1,
                        StepStageCompletionPolicy.ALL_MUST_APPROVE,
                        null,
                        null,
                        List.of(
                                new EngineeringChangeService.StepAssigneeDraft(EngineeringChangeStepAssigneeType.USER, actorId),
                                new EngineeringChangeService.StepAssigneeDraft(EngineeringChangeStepAssigneeType.USER, secondReviewerId)
                        )
                ))
        );

        assertEquals(1, ec.getStages().size());
        assertEquals(reviewStage.getId(), ec.getStages().getFirst().getId());
        assertEquals(2, ec.getSteps().size());
        assertEquals(existingStep.getId(), ec.getSteps().getFirst().getId());
        assertEquals(
                1,
                ec.getSteps().stream()
                        .filter(step -> step.getAssigneeId().equals(secondReviewerId))
                        .count()
        );
    }

    @Test
    void syncStages_같은Stage안에중복담당자가있으면_예외를던진다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(202, "변경", "본문", null, actorId);

        AppException exception = assertThrows(
                AppException.class,
                () -> engineeringChangeService.syncStages(
                        actorId,
                        ec,
                        List.of(new EngineeringChangeService.StageDraft(
                                null,
                                EngineeringChangeStepType.REVIEW,
                                1,
                                StepStageCompletionPolicy.ALL_MUST_APPROVE,
                                null,
                                null,
                                List.of(
                                        new EngineeringChangeService.StepAssigneeDraft(EngineeringChangeStepAssigneeType.USER, actorId),
                                        new EngineeringChangeService.StepAssigneeDraft(EngineeringChangeStepAssigneeType.USER, actorId)
                                )
                        ))
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void cancelEngineeringChange_후에는다시submit할수없다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange ec = EngineeringChange.create(105, "변경", "본문", null, actorId);

        StepStage reviewStage = ec.addStage(
                EngineeringChangeStepType.REVIEW, 1,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        ec.addStep(reviewStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        StepStage approvalStage = ec.addStage(
                EngineeringChangeStepType.APPROVAL, 2,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        ec.addStep(approvalStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        StepStage releaseStage = ec.addStage(
                EngineeringChangeStepType.RELEASE, 3,
                StepStageCompletionPolicy.ALL_MUST_APPROVE, null, null, actorId);
        ec.addStep(releaseStage, EngineeringChangeStepAssigneeType.USER, actorId, actorId);

        engineeringChangeService.cancelEngineeringChange(actorId, ec);

        AppException exception = assertThrows(
                AppException.class,
                () -> engineeringChangeService.submitEngineeringChange(actorId, ec)
        );

        assertEquals(ErrorCode.INVALID_STATE, exception.getErrorCode());
        assertEquals(EngineeringChangeState.CANCELED, ec.getState());
    }
}
