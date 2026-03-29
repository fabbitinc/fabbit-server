package com.fabbitinc.server.application.notification.usecase.command;

import java.util.UUID;

public record CreateReleaseNotificationsCommand(
        UUID engineeringChangeId,
        UUID actorId,
        int ecNumber,
        String ecTitle
) {
}
