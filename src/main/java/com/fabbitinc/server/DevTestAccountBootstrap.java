package com.fabbitinc.server;

import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.user.model.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevTestAccountBootstrap implements CommandLineRunner {

    // TODO 삭제: 개발용 테스트 계정/조직 부트스트랩
    private static final String TEST_EMAIL = "test@gmail.com";
    private static final String TEST_PASSWORD = "qwer1234";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_ORGANIZATION_SLUG = "test";
    private static final String TEST_ORGANIZATION_NAME = "Test Org";

    private final UserService userService;
    private final OrganizationApi organizationApi;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionApi subscriptionApi;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        try {
            transactionTemplate.executeWithoutResult(status -> bootstrapOnce());
        } catch (Exception ex) {
            log.warn("테스트 계정 보장 실패: {}", ex.getMessage(), ex);
        }
    }

    private void bootstrapOnce() {
        User user = ensureUser();

        Organization organization;
        Organization existingOrganization = organizationRepository.findBySlug(TEST_ORGANIZATION_SLUG).orElse(null);
        if (existingOrganization == null) {
            organization = organizationApi.createWorkspace(
                    user.getId(),
                    new CreateOrganizationInput(
                            TEST_ORGANIZATION_NAME,
                            TEST_ORGANIZATION_SLUG,
                            null,
                            null,
                            WorkspacePlanType.STARTER,
                            null
                    )
            );
        } else {
            organization = existingOrganization;
        }

        ensureActiveSubscription(organization);
        ensureOwnerMembership(user.getId(), organization.getId());

        log.info("테스트 계정 보장 완료: email={}, slug={}", TEST_EMAIL, TEST_ORGANIZATION_SLUG);
    }

    private User ensureUser() {
        return userService.getUserByEmail(TEST_EMAIL)
                .orElseGet(() -> userService.createUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME));
    }

    private void ensureActiveSubscription(Organization organization) {
        if (subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }
        membershipRepository.findByUserIdAndOrgId(organization.getOwnerId(), organization.getId())
                .ifPresent(membership -> subscriptionApi.createInitialSubscription(
                        organization.getId(),
                        WorkspacePlanType.STARTER,
                        membership,
                        null,
                        organization.getOwnerId()
                ));
    }

    private void ensureOwnerMembership(UUID userId, UUID orgId) {
        if (membershipRepository.findByUserIdAndOrgId(userId, orgId).isPresent()) {
            return;
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("조직을 찾을 수 없습니다: " + orgId));
        var membership = membershipRepository.save(organization.addMember(userId, MembershipRole.OWNER, null));
        organizationRepository.reserveMemberSeat(orgId);
        subscriptionApi.createInitialSubscription(orgId, WorkspacePlanType.STARTER, membership, null, userId);
    }
}
