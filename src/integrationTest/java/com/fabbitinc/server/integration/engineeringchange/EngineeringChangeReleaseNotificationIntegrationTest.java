package com.fabbitinc.server.integration.engineeringchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReleaseEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SubmitEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangeAffectedItemsUseCase;
import com.fabbitinc.server.application.issue.usecase.CreateIssueUseCase;
import com.fabbitinc.server.application.issue.usecase.SyncAssigneesUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import com.fabbitinc.server.domain.notification.repository.NotificationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
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
    @Autowired private CreateIssueUseCase createIssueUseCase;
    @Autowired private SyncAssigneesUseCase syncAssigneesUseCase;
    @Autowired private NotificationRepository notificationRepository;

    private EngineeringChangeIntegrationFixture fixture;
    private User actor;
    private User reviewer;

    @BeforeEach
    void setUp() {
        fixture = new EngineeringChangeIntegrationFixture(
                userRepository, testCurrentAuthProvider, null,
                createPartUseCase, createPartDraftUseCase, releasePartDraftUseCase,
                null, null,
                createEngineeringChangeUseCase, syncAffectedItemsUseCase, replaceStepsUseCase,
                submitUseCase, null, null, releaseUseCase, null
        );
        actor = fixture.createUser();
        reviewer = fixture.createUser();
        fixture.setAuth(actor);
    }

    @Test
    void EC_릴리즈시_연결이슈_담당자에게_RELEASE_알림이_생성된다() {
        // given
        PartWithReleasedRevision part = fixture.createPartWithReleasedRevision("NOTI-001", "알림 테스트 부품");
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
                        new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.Item(EngineeringChangeStepType.RELEASE, EngineeringChangeStepAssigneeType.USER, actor.getId(), 1)
                )
        ));
        submitUseCase.execute(new SubmitEngineeringChangeUseCase.SubmitEngineeringChangeCommand(ecId));

        // when
        releaseUseCase.execute(new ReleaseEngineeringChangeUseCase.ReleaseEngineeringChangeCommand(ecId));

        // then
        var notifications = notificationRepository.findAll().stream()
                .filter(notification -> reviewer.getId().equals(notification.getUserId()))
                .toList();
        assertEquals(1, notifications.size());
        assertEquals(NotificationType.RELEASE, notifications.getFirst().getType());
        assertTrue(notifications.getFirst().getPayload().contains("릴리즈 알림 EC"));
    }
}
