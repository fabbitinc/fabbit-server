package com.fabbitinc.server.application.notification.usecase.command;

import java.util.Set;
import java.util.UUID;

public record CreateMentionNotificationsCommand(
        UUID actorId,
        Set<UUID> mentionedUserIds,
        UUID sourceIssueId,
        int sourceNumber,
        String sourceTitle,
        String sourceIssueType,
        boolean comment
) {
    public CreateMentionNotificationsCommand {
        mentionedUserIds = mentionedUserIds == null ? Set.of() : Set.copyOf(mentionedUserIds);
    }
}
