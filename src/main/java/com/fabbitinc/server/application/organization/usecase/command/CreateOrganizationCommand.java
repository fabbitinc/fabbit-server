package com.fabbitinc.server.application.organization.usecase.command;

public record CreateOrganizationCommand(
        String orgName,
        String slug,
        String industry,
        String teamSize,
        String planType
) {
}
