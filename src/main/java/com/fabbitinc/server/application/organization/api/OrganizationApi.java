package com.fabbitinc.server.application.organization.api;

import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.domain.organization.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationApi {

    private final OrganizationService organizationService;

    public Organization createOrganization(UUID userId, CreateOrganizationInput input) {
        return organizationService.createOrganization(userId, input);
    }
}
