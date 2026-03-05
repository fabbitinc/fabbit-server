package com.fabbitinc.server.application.organization.usecase.command;

import java.util.UUID;

public record CancelInvitationCommand(
        UUID invitationId
) {
}
