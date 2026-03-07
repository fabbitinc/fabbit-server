package com.fabbitinc.server.application.organization.service;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.organization.model.WorkspaceSlugPolicy;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final long GB_TO_BYTES = 1_000_000_000L;
    private static final String STORAGE_QUOTA_EXCEEDED_MESSAGE = "스토리지 한도를 초과했습니다. 플랜을 업그레이드해주세요.";

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final TenantProvisioningPort tenantProvisioningPort;
    private final SubscriptionApi subscriptionApi;

    public Organization createOrganization(UUID userId, CreateOrganizationInput input) {
        PlanType planType = input.planType();
        if (planType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 플랜입니다");
        }

        String slug = resolveAvailableSlug(input.slug(), input.orgName());

        Organization organization = Organization.create(
                slug,
                input.orgName(),
                userId,
                input.industry(),
                input.teamSize(),
                planType,
                planType.maxMembers(),
                planType.aiCredits(),
                planType.storageGb() * GB_TO_BYTES
        );
        Membership ownerMembership = organization.addMember(userId, MembershipRole.OWNER, null);
        organization.reserveMemberSeat();

        organizationRepository.save(organization);

        membershipRepository.save(ownerMembership);
        tenantProvisioningPort.provisionTenant(organization.getId());
        subscriptionApi.createInitialSubscription(organization.getId(), planType);

        return organization;
    }

    public Membership switchOrganization(UUID userId, String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "해당 워크스페이스에 소속되어 있지 않습니다"));
        return membershipRepository.findByUserIdAndOrgId(userId, organization.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "해당 워크스페이스에 소속되어 있지 않습니다"));
    }

    public Organization getOrgOrThrow(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));
    }

    public Membership getMembershipOrThrow(UUID userId, UUID orgId) {
        return membershipRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "해당 멤버를 찾을 수 없습니다"));
    }

    public Membership getFirstMembershipOrThrow(UUID userId) {
        return membershipRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "소속된 조직이 없습니다"));
    }

    public List<Membership> getMembershipsByUserId(UUID userId) {
        return membershipRepository.findByUserId(userId);
    }

    public List<Membership> getMembershipsOrdered(UUID orgId) {
        return membershipRepository.findByOrgId(orgId).stream()
                .sorted(Comparator
                        .comparing((Membership membership) -> roleOrder(membership.getRole()))
                        .thenComparing(Membership::getUserId))
                .toList();
    }

    public void checkNotMember(UUID orgId, UUID userId) {
        if (membershipRepository.findByUserIdAndOrgId(userId, orgId).isPresent()) {
            throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 조직에 소속된 멤버입니다");
        }
    }

    public Membership addMember(UUID userId, UUID orgId, MembershipRole role) {
        if (role == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 역할입니다");
        }
        if (membershipRepository.findByUserIdAndOrgId(userId, orgId).isPresent()) {
            throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 조직에 소속된 멤버입니다");
        }

        if (organizationRepository.reserveMemberSeat(orgId) < 1) {
            throw new AppException(ErrorCode.MEMBER_LIMIT_EXCEEDED, "멤버 수 한도를 초과했습니다. 플랜을 업그레이드해주세요.");
        }

        Organization organization = getOrgOrThrow(orgId);
        return membershipRepository.save(organization.addMember(userId, role, null));
    }

    public void checkCreditQuota(UUID orgId, AiUsageCategory category) {
        Organization organization = getOrgOrThrow(orgId);
        if (organization.getPlanCreditsRemaining() + organization.getBonusCreditsRemaining() < category.creditCost()) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED, "AI 크레딧이 부족합니다. 플랜을 업그레이드해주세요.");
        }
    }

    public void consumeCredits(UUID orgId, AiUsageCategory category) {
        Organization organization = organizationRepository.findByIdForUpdate(orgId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));
        try {
            organization.useCredits(category.creditCost());
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED, "AI 크레딧이 부족합니다. 플랜을 업그레이드해주세요.");
        }
    }

    public void checkStorageQuota(UUID orgId, long additionalBytes) {
        long bytes = requireNonNegativeStorageBytes(additionalBytes);
        if (bytes == 0L) {
            return;
        }

        Organization organization = getOrgOrThrow(orgId);
        if (!organization.isAllowStorageOverage()
                && organization.getStorageBytesUsed() + bytes > organization.getStorageBytesLimit()) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED, STORAGE_QUOTA_EXCEEDED_MESSAGE);
        }
    }

    public void consumeStorage(UUID orgId, long deltaBytes) {
        long bytes = requireNonNegativeStorageBytes(deltaBytes);
        if (bytes == 0L) {
            return;
        }

        if (organizationRepository.consumeStorageBytes(orgId, bytes) < 1) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED, STORAGE_QUOTA_EXCEEDED_MESSAGE);
        }
    }

    public void releaseStorage(UUID orgId, long deltaBytes) {
        long bytes = requireNonNegativeStorageBytes(deltaBytes);
        if (bytes == 0L) {
            return;
        }

        organizationRepository.releaseStorageBytes(orgId, bytes);
    }

    public void removeMember(AuthContext auth, UUID userId) {
        if (auth.userId().equals(userId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "자신을 제거할 수 없습니다");
        }

        Membership target = getMembershipOrThrow(userId, auth.orgId());

        if (!auth.role().canManage(target.getRole())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 멤버를 제거할 권한이 없습니다");
        }

        if (target.getRole() == MembershipRole.OWNER && membershipRepository.countByOrgIdAndRole(auth.orgId(), MembershipRole.OWNER) <= 1) {
            throw new AppException(ErrorCode.FORBIDDEN, "마지막 소유자는 제거할 수 없습니다");
        }

        membershipRepository.deleteByOrgIdAndUserId(auth.orgId(), userId);
        organizationRepository.releaseMemberSeat(auth.orgId());
    }

    public Membership changeMemberRole(AuthContext auth, UUID userId, MembershipRole requestedRole) {
        if (auth.userId().equals(userId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "자신의 역할은 변경할 수 없습니다");
        }

        MembershipRole newRole = requestedRole;
        if (newRole == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 역할입니다");
        }

        Membership target = getMembershipOrThrow(userId, auth.orgId());

        if (target.getRole() == newRole) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "이미 해당 역할입니다");
        }

        long ownerCount = membershipRepository.countByOrgIdAndRole(auth.orgId(), MembershipRole.OWNER);
        Organization organization = getOrgOrThrow(auth.orgId());
        try {
            organization.changeMemberRole(target, newRole, ownerCount);
            return target;
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.FORBIDDEN, ex.getMessage());
        }
    }

    public void setProfileImage(AuthContext auth, File file) {
        Organization organization = getOrgOrThrow(auth.orgId());
        organization.changeProfileImage(file.getFileKey());
        file.assignOwner("organization", organization.getId());
        if (file.getFileSize() > 0L) {
            consumeStorage(auth.orgId(), file.getFileSize());
        }
    }

    public void deleteProfileImage(AuthContext auth) {
        Organization organization = getOrgOrThrow(auth.orgId());
        organization.removeProfileImage();
    }

    private String resolveAvailableSlug(String requestedSlug, String orgName) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String normalized = requestedSlug.trim().toLowerCase(Locale.ROOT);
            validateSlugFormat(normalized);
            if (organizationRepository.existsBySlug(normalized)) {
                throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 사용 중인 워크스페이스 주소입니다");
            }
            return normalized;
        }

        String generated = slugify(orgName);
        if (generated.isBlank()) {
            generated = "org";
        }

        if (!organizationRepository.existsBySlug(generated) && WorkspaceSlugPolicy.validateFormat(generated) == null) {
            return generated;
        }

        for (int i = 0; i < 100; i++) {
            String candidate = generated + "-" + UUID.randomUUID().toString().substring(0, 8);
            if (WorkspaceSlugPolicy.validateFormat(candidate) == null && !organizationRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }

        throw new AppException(ErrorCode.ALREADY_EXISTS, "워크스페이스 주소를 생성할 수 없습니다");
    }

    private void validateSlugFormat(String slug) {
        String validationError = WorkspaceSlugPolicy.validateFormat(slug);
        if (validationError != null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, validationError);
        }
    }

    private int roleOrder(MembershipRole role) {
        if (role == MembershipRole.OWNER) {
            return 0;
        }
        if (role == MembershipRole.ADMIN) {
            return 1;
        }
        return 2;
    }

    private String slugify(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT)
                .trim();

        String slug = normalized
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[-\\s]+", "-");

        if (slug.length() > 50) {
            slug = slug.substring(0, 50).replaceAll("-+$", "");
        }

        return slug;
    }

    private long requireNonNegativeStorageBytes(long bytes) {
        if (bytes < 0L) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "스토리지 사용량은 0 이상이어야 합니다");
        }
        return bytes;
    }
}
