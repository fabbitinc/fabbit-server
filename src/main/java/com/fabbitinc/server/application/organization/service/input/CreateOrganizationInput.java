package com.fabbitinc.server.application.organization.service.input;

public record CreateOrganizationInput(
        String orgName,
        String slug,
        String industry,
        String teamSize,
        String planType
) {
}
