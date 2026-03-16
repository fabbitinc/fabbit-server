package com.fabbitinc.server.application.engineeringchange.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeCommentRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeStepRepository;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.team.model.TeamMember;
import com.fabbitinc.server.domain.team.repository.TeamMemberRepository;
import com.fabbitinc.server.domain.team.repository.TeamRepository;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.domain.workitem.repository.WorkItemNumberSequenceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
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

    private EngineeringChangeService engineeringChangeService;

    @BeforeEach
    void setUp() {
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
                objectMapper
        );

        when(engineeringChangeRepository.existsById(any())).thenReturn(true);
        when(issueApi.existsIssue(any())).thenReturn(false);
    }

    @Test
    void approveReviewStep_다중ReviewStep이면_모두승인되기전까지ReviewPending을유지한다() {
        UUID actorId = UUID.randomUUID();
        UUID firstReviewerId = UUID.randomUUID();
        UUID secondReviewerId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(101, "변경", "본문", actorId);
        EngineeringChangeStep firstStep = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                firstReviewerId,
                1,
                actorId
        );
        EngineeringChangeStep secondStep = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                secondReviewerId,
                1,
                actorId
        );
        engineeringChange.submit(actorId);

        stubPendingSteps(engineeringChange, List.of(firstStep, secondStep));
        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of());

        engineeringChangeService.approveReviewStep(firstReviewerId, engineeringChange);

        assertEquals(EngineeringChangeStepStatus.APPROVED, firstStep.getStatus());
        assertEquals(EngineeringChangeStepStatus.PENDING, secondStep.getStatus());
        assertEquals(EngineeringChangeState.REVIEW_PENDING, engineeringChange.getState());

        engineeringChangeService.approveReviewStep(secondReviewerId, engineeringChange);

        assertEquals(EngineeringChangeStepStatus.APPROVED, secondStep.getStatus());
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, engineeringChange.getState());
    }

    @Test
    void approveReviewStep_팀ReviewStep이면_팀원사용자가승인할수있다() {
        UUID actorId = UUID.randomUUID();
        Team team = Team.create("검토팀", null, actorId);
        TeamMember teamMember = team.addMember(actorId);
        EngineeringChange engineeringChange = EngineeringChange.create(102, "변경", "본문", actorId);
        EngineeringChangeStep reviewStep = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.TEAM,
                team.getId(),
                1,
                actorId
        );
        engineeringChange.submit(actorId);

        stubPendingSteps(engineeringChange, List.of(reviewStep));
        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of(teamMember));

        engineeringChangeService.approveReviewStep(actorId, engineeringChange);

        assertEquals(EngineeringChangeStepStatus.APPROVED, reviewStep.getStatus());
        assertEquals(EngineeringChangeState.APPROVAL_PENDING, engineeringChange.getState());
    }

    @Test
    void rejectEngineeringChange_approvalPending이면Draft로되돌아간다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(103, "변경", "본문", actorId);
        EngineeringChangeStep approvalStep = engineeringChange.addStep(
                EngineeringChangeStepType.APPROVAL,
                EngineeringChangeStepAssigneeType.USER,
                actorId,
                1,
                actorId
        );
        engineeringChange.submit(actorId);
        engineeringChange.completeReview(actorId);

        stubPendingSteps(engineeringChange, List.of(approvalStep));
        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of());

        engineeringChangeService.rejectEngineeringChange(actorId, engineeringChange);

        assertEquals(EngineeringChangeStepStatus.REJECTED, approvalStep.getStatus());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getState());
    }

    @Test
    void rejectEngineeringChange_releasePending이면Draft로되돌아간다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(104, "변경", "본문", actorId);
        EngineeringChangeStep releaseStep = engineeringChange.addStep(
                EngineeringChangeStepType.RELEASE,
                EngineeringChangeStepAssigneeType.USER,
                actorId,
                1,
                actorId
        );
        engineeringChange.submit(actorId);
        engineeringChange.completeReview(actorId);
        engineeringChange.approve(actorId);

        stubPendingSteps(engineeringChange, List.of(releaseStep));
        when(teamMemberRepository.findByTeam_IdIn(anyCollection())).thenReturn(List.of());

        engineeringChangeService.rejectEngineeringChange(actorId, engineeringChange);

        assertEquals(EngineeringChangeStepStatus.REJECTED, releaseStep.getStatus());
        assertEquals(EngineeringChangeState.DRAFT, engineeringChange.getState());
    }

    @Test
    void cancelEngineeringChange_후에는다시submit할수없다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(105, "변경", "본문", actorId);
        EngineeringChangeStep reviewStep = engineeringChange.addStep(
                EngineeringChangeStepType.REVIEW,
                EngineeringChangeStepAssigneeType.USER,
                actorId,
                1,
                actorId
        );
        EngineeringChangeStep approvalStep = engineeringChange.addStep(
                EngineeringChangeStepType.APPROVAL,
                EngineeringChangeStepAssigneeType.USER,
                actorId,
                1,
                actorId
        );
        EngineeringChangeStep releaseStep = engineeringChange.addStep(
                EngineeringChangeStepType.RELEASE,
                EngineeringChangeStepAssigneeType.USER,
                actorId,
                1,
                actorId
        );
        when(engineeringChangeStepRepository.findByEngineeringChangeIdOrderBySequenceAscCreatedAtAsc(engineeringChange.getId()))
                .thenReturn(List.of(reviewStep, approvalStep, releaseStep));

        engineeringChangeService.cancelEngineeringChange(actorId, engineeringChange);

        AppException exception = assertThrows(
                AppException.class,
                () -> engineeringChangeService.submitEngineeringChange(actorId, engineeringChange)
        );

        assertEquals(ErrorCode.INVALID_STATE, exception.getErrorCode());
        assertEquals(EngineeringChangeState.CANCELED, engineeringChange.getState());
    }

    private void stubPendingSteps(EngineeringChange engineeringChange, List<EngineeringChangeStep> steps) {
        when(engineeringChangeStepRepository.findByEngineeringChangeIdAndStepTypeAndStatusOrderBySequenceAscCreatedAtAsc(
                eq(engineeringChange.getId()),
                any(EngineeringChangeStepType.class),
                eq(EngineeringChangeStepStatus.PENDING)
        )).thenAnswer(invocation -> {
            EngineeringChangeStepType stepType = invocation.getArgument(1);
            return steps.stream()
                    .filter(step -> step.getStepType() == stepType)
                    .filter(step -> step.getStatus() == EngineeringChangeStepStatus.PENDING)
                    .sorted(java.util.Comparator
                            .comparingInt(EngineeringChangeStep::getSequence)
                            .thenComparing(EngineeringChangeStep::getId))
                    .toList();
        });
    }
}
