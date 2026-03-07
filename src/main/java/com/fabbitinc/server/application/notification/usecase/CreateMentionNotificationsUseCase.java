package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.notification.event.NotificationCreatedEvent;
import com.fabbitinc.server.application.notification.service.NotificationService;
import com.fabbitinc.server.application.notification.usecase.command.CreateMentionNotificationsCommand;
import com.fabbitinc.server.domain.notification.model.Notification;
import com.fabbitinc.server.domain.notification.model.NotificationType;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class CreateMentionNotificationsUseCase {

    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public void execute(CreateMentionNotificationsCommand command) {
        for (java.util.UUID userId : command.mentionedUserIds()) {
            Notification notification = notificationService.create(
                    userId,
                    NotificationType.MENTION,
                    command.actorId(),
                    toMentionPayload(command)
            );
            applicationEventPublisher.publishEvent(
                    NotificationCreatedEvent.create(notification.getId(), notification.getUserId())
            );
        }
    }

    private String toMentionPayload(CreateMentionNotificationsCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source_issue_id", command.sourceIssueId().toString());
        payload.put("source_number", command.sourceNumber());
        payload.put("source_title", command.sourceTitle());
        payload.put("source_issue_type", command.sourceIssueType());
        payload.put("is_comment", command.comment());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 payload 직렬화에 실패했습니다");
        }
    }
}
