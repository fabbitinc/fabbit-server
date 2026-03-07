package com.fabbitinc.server.application.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.notification.event.NotificationCreatedEvent;
import com.fabbitinc.server.application.notification.service.NotificationService;
import com.fabbitinc.server.application.notification.usecase.command.CreateMentionNotificationsCommand;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CreateMentionNotificationsUseCaseTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CreateMentionNotificationsUseCase createMentionNotificationsUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 멘션된_사용자마다_알림을_저장하고_생성이벤트를_발행한다() throws Exception {
        createMentionNotificationsUseCase = new CreateMentionNotificationsUseCase(
                notificationService,
                applicationEventPublisher,
                objectMapper
        );

        UUID actorId = UUID.randomUUID();
        UUID issueId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        when(notificationService.create(
                org.mockito.ArgumentMatchers.eq(userId1),
                org.mockito.ArgumentMatchers.eq(NotificationType.MENTION),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(Notification.create(userId1, NotificationType.MENTION, actorId, "{\"ok\":true}"));
        when(notificationService.create(
                org.mockito.ArgumentMatchers.eq(userId2),
                org.mockito.ArgumentMatchers.eq(NotificationType.MENTION),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(Notification.create(userId2, NotificationType.MENTION, actorId, "{\"ok\":true}"));

        createMentionNotificationsUseCase.execute(new CreateMentionNotificationsCommand(
                actorId,
                Set.of(userId1, userId2),
                issueId,
                101,
                "제목",
                "issue",
                true
        ));

        verify(notificationService, times(2)).create(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq(NotificationType.MENTION),
                org.mockito.ArgumentMatchers.eq(actorId),
                argThat(payload -> {
                    try {
                        var node = objectMapper.readTree(payload);
                        return node.get("source_issue_id").asText().equals(issueId.toString())
                                && node.get("source_number").asInt() == 101
                                && node.get("source_title").asText().equals("제목")
                                && node.get("source_issue_type").asText().equals("issue")
                                && node.get("is_comment").asBoolean();
                    } catch (Exception ex) {
                        return false;
                    }
                })
        );

        ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertEquals(2, eventCaptor.getAllValues().size());
    }
}
