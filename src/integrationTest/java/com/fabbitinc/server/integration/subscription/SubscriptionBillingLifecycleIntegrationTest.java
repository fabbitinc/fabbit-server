package com.fabbitinc.server.integration.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.usecase.AcceptInvitationUseCase;
import com.fabbitinc.server.application.member.usecase.ChangeMemberSeatUseCase;
import com.fabbitinc.server.application.organization.usecase.CreateInvitationUseCase;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.ProcessPendingSubscriptionPaymentsUseCase;
import com.fabbitinc.server.application.subscription.usecase.RecordStorageUsageSnapshotsUseCase;
import com.fabbitinc.server.application.subscription.usecase.RenewSubscriptionsUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpgradeStarterSubscriptionUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionSeatQuotasUseCase;
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
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatQuotaRepository;
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
    private AuthInvitationService authInvitationService;
    @Autowired
    private CreateInvitationUseCase createInvitationUseCase;
    @Autowired
    private AcceptInvitationUseCase acceptInvitationUseCase;
    @Autowired
    private AiUsageService aiUsageService;
    @Autowired
    private ChangeMemberSeatUseCase changeMemberSeatUseCase;
    @Autowired
    private UpgradeStarterSubscriptionUseCase upgradeStarterSubscriptionUseCase;
    @Autowired
    private UpdateSubscriptionSeatQuotasUseCase updateSubscriptionSeatQuotasUseCase;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionSeatQuotaRepository subscriptionSeatQuotaRepository;
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
                authInvitationService,
                createInvitationUseCase,
                acceptInvitationUseCase,
                subscriptionRepository,
                subscriptionSeatQuotaRepository,
                changeMemberSeatUseCase,
                upgradeStarterSubscriptionUseCase,
                updateSubscriptionSeatQuotasUseCase,
                testCurrentAuthProvider
        );
    }

    @Test
    void team_플랜은_ai_사용량과_스토리지_초과분이_갱신후_청구되고_mock_pg로_정산된다() {
        var owner = fixture.createUser("team-owner@example.com");
        var member = fixture.createUser("team-member@example.com");
        var organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM);
        fixture.updateSeatQuota(organization, SeatType.FULL, 2, owner, MembershipRole.OWNER);
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

    @Test
    void 같은_청구기간에_viewer를_full로_전환하면_좌석차액_adjustment만_추가되고_다음갱신에는_full만_청구된다() {
        var owner = fixture.createUser("seat-upgrade-owner@example.com");
        var organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM, SeatType.VIEWER);
        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).orElseThrow();

        fixture.updateSeatQuota(organization, SeatType.FULL, 1, owner, MembershipRole.OWNER);
        fixture.assignSeat(organization, owner, SeatType.FULL, owner, MembershipRole.OWNER);
        fixture.updateSeatQuota(organization, SeatType.VIEWER, 0, owner, MembershipRole.OWNER);

        var pendingLedgers = subscriptionBillingLedgerRepository.findByOrgIdAndStatus(
                organization.getId(),
                SubscriptionBillingLedgerStatus.PENDING
        );
        assertTrue(pendingLedgers.stream().anyMatch(ledger ->
                ledger.getLedgerType() == SubscriptionBillingLedgerType.ADJUSTMENT
                        && ledger.getTotalAmount().signum() > 0
                        && "seat_proration_full".equals(ledger.getReferenceType())
        ));
        assertTrue(pendingLedgers.stream().anyMatch(ledger ->
                ledger.getLedgerType() == SubscriptionBillingLedgerType.ADJUSTMENT
                        && ledger.getTotalAmount().signum() < 0
                        && "seat_proration_viewer".equals(ledger.getReferenceType())
        ));

        ReflectionTestUtils.setField(subscription, "currentPeriodStart", Instant.now().minus(31, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(subscription, "currentPeriodEnd", Instant.now().minus(1, ChronoUnit.MINUTES));
        subscriptionRepository.save(subscription);

        renewSubscriptionsUseCase.execute();

        var renewedSubscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).orElseThrow();
        var seatLedgers = subscriptionBillingLedgerRepository.findBySubscriptionIdAndLedgerType(
                renewedSubscription.getId(),
                SubscriptionBillingLedgerType.SEAT
        );
        Instant renewedPeriodStart = renewedSubscription.getCurrentPeriodStart();
        assertTrue(seatLedgers.stream().anyMatch(ledger ->
                "seat_full".equals(ledger.getReferenceType())
                        && renewedPeriodStart.equals(ledger.getPeriodStart())
        ));
        assertFalse(seatLedgers.stream().anyMatch(ledger ->
                "seat_viewer".equals(ledger.getReferenceType())
                        && renewedPeriodStart.equals(ledger.getPeriodStart())
        ));
        assertEquals(SeatType.FULL, subscriptionApi.getCurrentSeatType(organization.getId(), owner.getId()));
        assertTrue(renewedSubscription.getCurrentPeriodEnd().isAfter(renewedPeriodStart));
    }

    @Test
    void 같은_청구기간에_full을_viewer로_전환하면_좌석차액_adjustment가_반영되고_다음갱신에는_viewer만_청구된다() {
        var owner = fixture.createUser("seat-downgrade-owner@example.com");
        var organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM, SeatType.FULL);
        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).orElseThrow();

        fixture.updateSeatQuota(organization, SeatType.VIEWER, 1, owner, MembershipRole.OWNER);
        fixture.assignSeat(organization, owner, SeatType.VIEWER, owner, MembershipRole.OWNER);
        fixture.updateSeatQuota(organization, SeatType.FULL, 0, owner, MembershipRole.OWNER);

        var pendingLedgers = subscriptionBillingLedgerRepository.findByOrgIdAndStatus(
                organization.getId(),
                SubscriptionBillingLedgerStatus.PENDING
        );
        assertTrue(pendingLedgers.stream().anyMatch(ledger ->
                ledger.getLedgerType() == SubscriptionBillingLedgerType.ADJUSTMENT
                        && ledger.getTotalAmount().signum() > 0
                        && "seat_proration_viewer".equals(ledger.getReferenceType())
        ));
        assertTrue(pendingLedgers.stream().anyMatch(ledger ->
                ledger.getLedgerType() == SubscriptionBillingLedgerType.ADJUSTMENT
                        && ledger.getTotalAmount().signum() < 0
                        && "seat_proration_full".equals(ledger.getReferenceType())
        ));

        ReflectionTestUtils.setField(subscription, "currentPeriodStart", Instant.now().minus(31, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(subscription, "currentPeriodEnd", Instant.now().minus(1, ChronoUnit.MINUTES));
        subscriptionRepository.save(subscription);

        renewSubscriptionsUseCase.execute();

        var renewedSubscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE).orElseThrow();
        var seatLedgers = subscriptionBillingLedgerRepository.findBySubscriptionIdAndLedgerType(
                renewedSubscription.getId(),
                SubscriptionBillingLedgerType.SEAT
        );
        Instant renewedPeriodStart = renewedSubscription.getCurrentPeriodStart();
        assertTrue(seatLedgers.stream().anyMatch(ledger ->
                "seat_viewer".equals(ledger.getReferenceType())
                        && renewedPeriodStart.equals(ledger.getPeriodStart())
        ));
        assertFalse(seatLedgers.stream().anyMatch(ledger ->
                "seat_full".equals(ledger.getReferenceType())
                        && renewedPeriodStart.equals(ledger.getPeriodStart())
        ));
        assertEquals(SeatType.VIEWER, subscriptionApi.getCurrentSeatType(organization.getId(), owner.getId()));
        assertTrue(renewedSubscription.getCurrentPeriodEnd().isAfter(renewedPeriodStart));
    }
}
