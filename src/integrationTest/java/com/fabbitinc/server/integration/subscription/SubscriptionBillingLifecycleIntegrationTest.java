package com.fabbitinc.server.integration.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.ProcessPendingSubscriptionPaymentsUseCase;
import com.fabbitinc.server.application.subscription.usecase.RecordStorageUsageSnapshotsUseCase;
import com.fabbitinc.server.application.subscription.usecase.RenewSubscriptionsUseCase;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.aiusage.model.AiUsageEvent;
import com.fabbitinc.server.domain.aiusage.repository.AiUsageEventRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.repository.StorageOverageLedgerRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionBillingLedgerRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.fixture.SubscriptionIntegrationFixture;
import com.fabbitinc.server.integration.support.PostgresIntegrationTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class SubscriptionBillingLifecycleIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SubscriptionApi subscriptionApi;
    @Autowired
    private AiUsageService aiUsageService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionBillingLedgerRepository subscriptionBillingLedgerRepository;
    @Autowired
    private StorageOverageLedgerRepository storageOverageLedgerRepository;
    @Autowired
    private AiUsageEventRepository aiUsageEventRepository;
    @Autowired
    private RecordStorageUsageSnapshotsUseCase recordStorageUsageSnapshotsUseCase;
    @Autowired
    private RenewSubscriptionsUseCase renewSubscriptionsUseCase;
    @Autowired
    private ProcessPendingSubscriptionPaymentsUseCase processPendingSubscriptionPaymentsUseCase;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SubscriptionIntegrationFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new SubscriptionIntegrationFixture(
                userRepository,
                organizationService,
                subscriptionApi,
                aiUsageService,
                testCurrentAuthProvider
        );
    }

    @Test
    void team_플랜은_ai_사용량과_스토리지_초과분이_갱신후_청구되고_mock_pg로_정산된다() {
        var owner = fixture.createUser("team-owner@example.com");
        var member = fixture.createUser("team-member@example.com");
        var organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM);
        fixture.addMember(member, organization, MembershipRole.MEMBER);
        fixture.assignSeat(organization, member, SeatType.FULL, owner, MembershipRole.OWNER);

        fixture.recordAiUsage(organization, member, AiUsageCategory.DRAWING_PARSE);
        fixture.runInTenantContext(organization.getId(), () -> {
            AiUsageEvent event = aiUsageEventRepository.findAll().getFirst();
            String schemaName = TenantSchemaPolicy.quoteIdentifier(TenantSchemaPolicy.schemaNameForOrgId(organization.getId()));
            jdbcTemplate.update(
                    "update " + schemaName + ".ai_usage_events set created_at = ? where id = ?",
                    Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)),
                    event.getId()
            );
        });
        organizationService.consumeStorage(organization.getId(), 35_000_000_000L);
        recordStorageUsageSnapshotsUseCase.execute();

        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).orElseThrow();
        ReflectionTestUtils.setField(subscription, "currentPeriodStart", Instant.now().minus(31, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(subscription, "currentPeriodEnd", Instant.now().minus(1, ChronoUnit.MINUTES));
        subscriptionRepository.save(subscription);

        var renewalResult = renewSubscriptionsUseCase.execute();
        assertEquals(1, renewalResult.renewedCount());
        assertEquals(0, renewalResult.failedCount());

        var pendingLedgers = subscriptionBillingLedgerRepository.findByOrgIdAndStatus(
                organization.getId(),
                SubscriptionBillingLedgerStatus.PENDING
        );
        assertFalse(pendingLedgers.isEmpty());
        assertTrue(pendingLedgers.stream().anyMatch(ledger -> ledger.getLedgerType() == SubscriptionBillingLedgerType.AI_USAGE));
        assertTrue(pendingLedgers.stream().anyMatch(ledger -> ledger.getLedgerType() == SubscriptionBillingLedgerType.SEAT));
        assertTrue(pendingLedgers.stream().anyMatch(ledger -> ledger.getLedgerType() == SubscriptionBillingLedgerType.STORAGE_OVERAGE));

        fixture.runInTenantContext(organization.getId(), () -> {
            var aiEvents = aiUsageEventRepository.findAll();
            assertEquals(1, aiEvents.size());
            AiUsageEvent event = aiEvents.getFirst();
            assertEquals(AiUsageEvent.BILLING_STATUS_BILLED, event.getBillingStatus());
        });

        processPendingSubscriptionPaymentsUseCase.execute();

        var settledLedgers = subscriptionBillingLedgerRepository.findByOrgIdAndStatus(
                organization.getId(),
                SubscriptionBillingLedgerStatus.SETTLED
        );
        assertEquals(pendingLedgers.size(), settledLedgers.size());
        assertTrue(storageOverageLedgerRepository.findByOrgIdAndStatus(
                organization.getId(),
                SubscriptionBillingLedgerStatus.SETTLED
        ).stream().findAny().isPresent());
    }
}
