package com.fabbitinc.server.application.auth.dto.response;

public record CreateOrganizationResponse(
        OrganizationResponse organization,
        TokenResponse tokens
) {
}
