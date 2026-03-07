package com.fabbitinc.server.application.notification.eventhandler;

import com.fabbitinc.server.application.issue.event.IssueUsersMentionedEvent;
import com.fabbitinc.server.application.notification.usecase.CreateMentionNotificationsUseCase;
import com.fabbitinc.server.application.notification.usecase.command.CreateMentionNotificationsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueUsersMentionedEventHandler {

    private final CreateMentionNotificationsUseCase createMentionNotificationsUseCase;

    @EventListener
    public void handle(IssueUsersMentionedEvent event) {
        log.debug(
                "event=issue_users_mentioned event_id={} event_type={} aggregate_id={} mentioned_count={}",
                event.eventId(),
                event.getClass().getSimpleName(),
                event.aggregateId(),
                event.mentionedUserIds().size()
        );
        createMentionNotificationsUseCase.execute(new CreateMentionNotificationsCommand(
                event.actorId(),
                event.mentionedUserIds(),
                event.aggregateId(),
                event.sourceNumber(),
                event.sourceTitle(),
                event.sourceIssueType(),
                event.comment()
        ));
    }
}
