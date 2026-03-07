package com.fabbitinc.server.application.member.usecase.command;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.UUID;

public record ChangeMemberRoleCommand(
        UUID userId,
        MembershipRole role
) {
}
