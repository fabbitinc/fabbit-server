package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record UpdatePartOwnerCommand(
        String partNumber,
        String revisionCode,
        UUID ownerId,
        boolean ownerIdSet,
        UUID ownerTeamId,
        boolean ownerTeamIdSet
) {
}
