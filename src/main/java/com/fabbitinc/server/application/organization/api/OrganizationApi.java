package com.fabbitinc.server.application.organization.api;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.List;

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

    public void changeMemberRole(AuthContext auth, UUID userId, String requestedRole) {
        organizationService.changeMemberRole(auth, userId, requestedRole);
    }

    public void removeMember(AuthContext auth, UUID userId) {
        organizationService.removeMember(auth, userId);
    }

    public Organization getOrganizationOrThrow(UUID orgId) {
        return organizationService.getOrgOrThrow(orgId);
    }

    public List<Membership> getMembershipsByUserId(UUID userId) {
        return organizationService.getMembershipsByUserId(userId);
    }

    public List<Membership> getMembershipsOrdered(UUID orgId) {
        return organizationService.getMembershipsOrdered(orgId);
    }
}
