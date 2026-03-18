package com.fabbitinc.server.application.organization.api;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationApi {

    private final OrganizationService organizationService;

    public Organization createWorkspace(UUID userId, CreateOrganizationInput input) {
        return organizationService.createWorkspace(userId, input);
    }

    public Membership switchOrganization(UUID userId, String slug) {
        return organizationService.switchOrganization(userId, slug);
    }

    public Membership addMember(UUID userId, UUID orgId, MembershipRole role) {
        return organizationService.addMember(userId, orgId, role);
    }

    public void checkCreditQuota(UUID orgId, AiUsageCategory category) {
        organizationService.checkCreditQuota(orgId, category);
    }

    public void consumeCredits(UUID orgId, AiUsageCategory category) {
        organizationService.consumeCredits(orgId, category);
    }

    public void checkStorageQuota(UUID orgId, long additionalBytes) {
        organizationService.checkStorageQuota(orgId, additionalBytes);
    }

    public void consumeStorage(UUID orgId, long deltaBytes) {
        organizationService.consumeStorage(orgId, deltaBytes);
    }

    public void releaseStorage(UUID orgId, long deltaBytes) {
        organizationService.releaseStorage(orgId, deltaBytes);
    }

    public void consumeStorageForCurrentTenant(long deltaBytes) {
        organizationService.consumeStorage(resolveCurrentOrgId(), deltaBytes);
    }

    public void releaseStorageForCurrentTenant(long deltaBytes) {
        organizationService.releaseStorage(resolveCurrentOrgId(), deltaBytes);
    }

    public void changeMemberRole(AuthContext auth, UUID userId, MembershipRole requestedRole) {
        organizationService.changeMemberRole(auth, userId, requestedRole);
    }

    public void removeMember(AuthContext auth, UUID userId) {
        organizationService.removeMember(auth, userId);
    }

    public Organization getOrganizationOrThrow(UUID orgId) {
        return organizationService.getOrgOrThrow(orgId);
    }

    public Membership getMembershipOrThrow(UUID userId, UUID orgId) {
        return organizationService.getMembershipOrThrow(userId, orgId);
    }

    public boolean hasOwnedOrganization(UUID userId) {
        return organizationService.hasOwnedOrganization(userId);
    }

    public List<Membership> getMembershipsByUserId(UUID userId) {
        return organizationService.getMembershipsByUserId(userId);
    }

    public List<Membership> getMembershipsOrdered(UUID orgId) {
        return organizationService.getMembershipsOrdered(orgId);
    }

    private UUID resolveCurrentOrgId() {
        String schemaName = TenantContextHolder.getCurrentSchema();
        try {
            return TenantSchemaPolicy.orgIdForSchemaName(schemaName);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "현재 조직 컨텍스트를 확인할 수 없습니다");
        }
    }
}
