package com.fabbitinc.server.application.organization.api;

import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
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

    public Membership switchOrganization(UUID userId, String slug) {
        return organizationService.switchOrganization(userId, slug);
    }

    public Membership addMember(UUID userId, UUID orgId, MembershipRole role) {
        return organizationService.addMember(userId, orgId, role);
    }
}
