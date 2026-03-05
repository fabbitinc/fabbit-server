package com.fabbitinc.server;

import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.OrganizationPlans;
import com.fabbitinc.server.domain.organization.model.PlanLimits;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevTestAccountBootstrap implements CommandLineRunner {

    private static final long GB_TO_BYTES = 1_000_000_000L;

    // TODO 삭제: 개발용 테스트 계정/조직 부트스트랩
    private static final String TEST_EMAIL = "test@gmail.com";
    private static final String TEST_PASSWORD = "qwer1234";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_ORG_SLUG = "test";
    private static final String TEST_ORG_NAME = "Test Org";

    private final UserService userService;
    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

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
        Organization existingOrganization = organizationRepository.findBySlug(TEST_ORG_SLUG).orElse(null);
        if (existingOrganization == null) {
            organization = organizationService.createOrganization(
                    user.getId(),
                    new CreateOrganizationInput(
                            TEST_ORG_NAME,
                            TEST_ORG_SLUG,
                            null,
                            null,
                            PlanType.STARTER
                    )
            );
        } else {
            organization = existingOrganization;
        }

        backfillOrganizationLimitsIfEmpty(organization);
        ensureActiveSubscription(organization);
        ensureOwnerMembership(user.getId(), organization.getId());

        log.info("테스트 계정 보장 완료: email={}, slug={}", TEST_EMAIL, TEST_ORG_SLUG);
    }

    private User ensureUser() {
        return userService.getUserByEmail(TEST_EMAIL)
                .orElseGet(() -> userService.createUser(TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME));
    }

    private void backfillOrganizationLimitsIfEmpty(Organization organization) {
        if (organization.getMaxMembers() != 0 || organization.getPlanCreditsRemaining() != 0) {
            return;
        }

        PlanLimits limits = resolvePlanLimits(organization.getPlanType());
        entityManager.createQuery("""
                        update Organization o
                        set o.maxMembers = :maxMembers,
                            o.planCreditsRemaining = :planCreditsRemaining,
                            o.storageBytesLimit = :storageBytesLimit
                        where o.id = :orgId
                          and o.maxMembers = 0
                          and o.planCreditsRemaining = 0
                        """)
                .setParameter("maxMembers", limits.maxMembers())
                .setParameter("planCreditsRemaining", limits.aiCredits())
                .setParameter("storageBytesLimit", (long) limits.storageGb() * GB_TO_BYTES)
                .setParameter("orgId", organization.getId())
                .executeUpdate();
    }

    private void ensureActiveSubscription(Organization organization) {
        if (subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }

        PlanType planType = organization.getPlanType() == null ? PlanType.STARTER : organization.getPlanType();
        PlanLimits limits = resolvePlanLimits(planType);
        Instant now = Instant.now();
        Instant periodEnd = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).plusMonths(1).toInstant();

        subscriptionRepository.save(
                Subscription.create(
                        organization.getId(),
                        planType.name(),
                        SubscriptionStatus.ACTIVE,
                        now,
                        periodEnd,
                        limits.maxMembers(),
                        limits.aiCredits(),
                        (long) limits.storageGb() * GB_TO_BYTES
                )
        );
    }

    private void ensureOwnerMembership(UUID userId, UUID orgId) {
        if (membershipRepository.findByUserIdAndOrgId(userId, orgId).isPresent()) {
            return;
        }

        membershipRepository.save(Membership.createOwner(userId, orgId));
        organizationRepository.reserveMemberSeat(orgId);
    }

    private PlanLimits resolvePlanLimits(PlanType planType) {
        if (planType == null) {
            return OrganizationPlans.limits().get(PlanType.STARTER);
        }
        return OrganizationPlans.limits().getOrDefault(planType, OrganizationPlans.limits().get(PlanType.STARTER));
    }
}
