package com.fabbitinc.server.application.issue.event;

import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import java.util.Set;
import java.util.UUID;

public record IssueUsersMentionedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID actorId,
        Set<UUID> mentionedUserIds,
        int sourceNumber,
        String sourceTitle,
        String sourceIssueType,
        boolean comment
) {

    public IssueUsersMentionedEvent {
        mentionedUserIds = mentionedUserIds == null ? Set.of() : Set.copyOf(mentionedUserIds);
    }

    public static IssueUsersMentionedEvent create(
            UUID aggregateId,
            UUID actorId,
            Set<UUID> mentionedUserIds,
            int sourceNumber,
            String sourceTitle,
            String sourceIssueType,
            boolean comment
    ) {
        return new IssueUsersMentionedEvent(
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
