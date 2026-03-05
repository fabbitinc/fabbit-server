package com.fabbitinc.server.application.organization.query.result;

import java.util.List;

public record OrganizationInvitationListResult(
        List<OrganizationInvitationResult> invitations
) {
}
