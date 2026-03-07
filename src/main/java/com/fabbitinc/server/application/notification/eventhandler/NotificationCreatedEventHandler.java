package com.fabbitinc.server.application.notification.eventhandler;

import com.fabbitinc.server.application.notification.event.NotificationCreatedEvent;
import com.fabbitinc.server.application.notification.usecase.PushNotificationStreamUseCase;
import com.fabbitinc.server.application.notification.usecase.command.PushNotificationStreamCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedEventHandler {

    private final PushNotificationStreamUseCase pushNotificationStreamUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationCreatedEvent event) {
        log.debug(
                "event=notification_created event_id={} event_type={} aggregate_id={} user_id={}",
                event.eventId(),
                event.getClass().getSimpleName(),
                event.aggregateId(),
                event.userId()
        );
        pushNotificationStreamUseCase.execute(new PushNotificationStreamCommand(
                event.aggregateId(),
                event.userId(),
                event.actorId(),
                event.actorFullName(),
                event.actorProfileImageFileKey(),
                event.sourceIssueId(),
                event.sourceNumber(),
                event.sourceTitle(),
                event.sourceIssueType(),
                event.comment()
        ));
    }
}
