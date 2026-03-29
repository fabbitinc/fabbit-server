package com.fabbitinc.server.application.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.notification.event.NotificationCreatedEvent;
import com.fabbitinc.server.application.notification.service.NotificationService;
import com.fabbitinc.server.application.notification.usecase.command.CreateReleaseNotificationsCommand;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.user.model.User;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CreateReleaseNotificationsUseCaseTest {

    @Mock private EngineeringChangeAffectedItemRepository engineeringChangeAffectedItemRepository;
    @Mock private PartRevisionRepository partRevisionRepository;
    @Mock private EngineeringBomItemRepository engineeringBomItemRepository;
    @Mock private IssueApi issueApi;
    @Mock private NotificationService notificationService;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    @Mock private EngineeringChangeIssueLinkRepository engineeringChangeIssueLinkRepository;
    @Mock private UserApi userApi;

    @InjectMocks
    private CreateReleaseNotificationsUseCase useCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 영향항목이_없으면_알림을_만들지_않는다() {
        UUID ecId = UUID.randomUUID();
        when(engineeringChangeAffectedItemRepository.findByEngineeringChangeIdAndItemTypeOrderByCreatedAtAsc(ecId, EngineeringChangeAffectedItemType.REVISION_RELEASE))
                .thenReturn(List.of());

        useCase = new CreateReleaseNotificationsUseCase(
                engineeringChangeAffectedItemRepository,
                partRevisionRepository,
                engineeringBomItemRepository,
                issueApi,
                notificationService,
                applicationEventPublisher,
                engineeringChangeIssueLinkRepository,
                userApi,
                objectMapper
        );

        useCase.execute(new CreateReleaseNotificationsCommand(ecId, UUID.randomUUID(), 1, "EC"));

        verify(notificationService, never()).create(any(), any(), any(), any());
    }

    @Test
    void 발행자는_제외되고_최대_50명까지만_알림을_생성한다() {
        UUID ecId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        UUID issueId = UUID.randomUUID();

        EngineeringChangeAffectedItem affectedItem = org.mockito.Mockito.mock(EngineeringChangeAffectedItem.class);
        when(affectedItem.getTargetId()).thenReturn(revisionId);
        when(engineeringChangeAffectedItemRepository.findByEngineeringChangeIdAndItemTypeOrderByCreatedAtAsc(ecId, EngineeringChangeAffectedItemType.REVISION_RELEASE))
                .thenReturn(List.of(affectedItem));

        Part part = Part.create("P-001");
        PartRevision revision = PartRevision.createOfficial(part, "A", null, "name", PartRevisionStatus.RELEASED, UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(revision, "id", revisionId);
        when(partRevisionRepository.findAllById(any())).thenReturn(List.of(revision));
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(revisionId)).thenReturn(List.of());

        EngineeringChangeIssueLink link = org.mockito.Mockito.mock(EngineeringChangeIssueLink.class);
        when(link.getIssueId()).thenReturn(issueId);
        when(engineeringChangeIssueLinkRepository.findByEngineeringChangeId(ecId)).thenReturn(List.of(link));

        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        recipients.add(actorId);
        for (int i = 0; i < 60; i++) {
            recipients.add(UUID.randomUUID());
        }
        when(issueApi.getIssueAssigneeUserIds(issueId)).thenReturn(recipients);
        when(issueApi.getIssueIdsByPartIds(any())).thenReturn(Set.of());

        User actor = User.create("actor@test.com", "hashed", "행위자");
        when(userApi.getUserOrNull(actorId)).thenReturn(actor);
        when(notificationService.create(any(), any(), any(), any())).thenAnswer(invocation ->
                Notification.create(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2), invocation.getArgument(3))
        );

        useCase = new CreateReleaseNotificationsUseCase(
                engineeringChangeAffectedItemRepository,
                partRevisionRepository,
                engineeringBomItemRepository,
                issueApi,
                notificationService,
                applicationEventPublisher,
                engineeringChangeIssueLinkRepository,
                userApi,
                objectMapper
        );

        useCase.execute(new CreateReleaseNotificationsCommand(ecId, actorId, 77, "릴리즈 EC"));

        ArgumentCaptor<UUID> recipientCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService, times(50)).create(
                recipientCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(NotificationType.RELEASE),
                org.mockito.ArgumentMatchers.eq(actorId),
                argThat(payload -> payload.contains("릴리즈 EC"))
        );
        assertEquals(50, recipientCaptor.getAllValues().size());
        org.junit.jupiter.api.Assertions.assertFalse(recipientCaptor.getAllValues().contains(actorId));

        verify(applicationEventPublisher, times(50)).publishEvent(any(NotificationCreatedEvent.class));
    }
}
