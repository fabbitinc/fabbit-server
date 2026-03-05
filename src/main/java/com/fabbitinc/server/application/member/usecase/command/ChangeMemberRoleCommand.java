package com.fabbitinc.server.application.member.usecase.command;

import java.util.UUID;

public record ChangeMemberRoleCommand(
        UUID userId,
        String role
) {
}
