package com.fabbitinc.server.application.organization.usecase.command;

import com.fabbitinc.server.domain.organization.model.MembershipRole;

public record CreateInvitationCommand(
        String email,
        MembershipRole role
) {
}
