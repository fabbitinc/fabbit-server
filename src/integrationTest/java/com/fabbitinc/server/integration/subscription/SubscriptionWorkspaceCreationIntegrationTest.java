package com.fabbitinc.server.integration.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatAssignmentRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatQuotaRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionUsagePolicyRepository;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.fixture.SubscriptionIntegrationFixture;
import com.fabbitinc.server.integration.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionWorkspaceCreationIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SubscriptionApi subscriptionApi;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionSeatAssignmentRepository subscriptionSeatAssignmentRepository;
    @Autowired
    private SubscriptionSeatQuotaRepository subscriptionSeatQuotaRepository;
    @Autowired
    private SubscriptionUsagePolicyRepository subscriptionUsagePolicyRepository;

    private SubscriptionIntegrationFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SubscriptionIntegrationFixture(
                userRepository,
                organizationService,
                subscriptionApi,
                null,
                testCurrentAuthProvider
        );
    }

    @Test
    void starter_워크스페이스_생성시_구독_정책과_좌석이_함께_생성된다() {
        var owner = fixture.createUser("starter-owner@example.com");

        Organization organization = fixture.createWorkspace(owner, WorkspacePlanType.STARTER);

        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow();
        var usagePolicy = subscriptionUsagePolicyRepository.findBySubscriptionId(subscription.getId()).orElseThrow();
        var seatAssignment = subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(organization.getId(), owner.getId()).orElseThrow();
        var seatQuota = subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), SeatType.STARTER).orElseThrow();

        assertEquals(WorkspacePlanType.STARTER, subscription.getPlanType());
        assertEquals(SeatType.STARTER, seatAssignment.getSeatType());
        assertEquals(WorkspacePlanType.STARTER.maxMembers(), seatQuota.getPurchasedQuantity());
        assertEquals(WorkspacePlanType.STARTER.baseStorageBytes(), usagePolicy.getBaseStorageBytes());
        assertEquals(WorkspacePlanType.STARTER.starterMonthlyAiCredits(), usagePolicy.getStarterMonthlyAiCredits().intValue());
        assertTrue(subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart()));
    }
}
