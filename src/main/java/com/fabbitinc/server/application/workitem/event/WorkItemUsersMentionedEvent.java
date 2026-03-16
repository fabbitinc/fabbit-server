package com.fabbitinc.server.application.workitem.event;

import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import java.util.Set;
import java.util.UUID;

public record WorkItemUsersMentionedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID actorId,
        Set<UUID> mentionedUserIds,
        int sourceNumber,
        String sourceTitle,
        String sourceIssueType,
        boolean comment
) {

    public WorkItemUsersMentionedEvent {
        mentionedUserIds = mentionedUserIds == null ? Set.of() : Set.copyOf(mentionedUserIds);
    }

    public static WorkItemUsersMentionedEvent create(
            UUID aggregateId,
            UUID actorId,
            Set<UUID> mentionedUserIds,
            int sourceNumber,
            String sourceTitle,
            String sourceIssueType,
            boolean comment
    ) {
        return new WorkItemUsersMentionedEvent(
                UuidV7Generator.next(),
                aggregateId,
                actorId,
                mentionedUserIds,
                sourceNumber,
                sourceTitle,
                sourceIssueType,
                comment
        );
    }
}
