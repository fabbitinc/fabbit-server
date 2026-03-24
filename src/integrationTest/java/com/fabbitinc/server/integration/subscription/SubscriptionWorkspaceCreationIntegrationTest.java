package com.fabbitinc.server.integration.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.usecase.AcceptInvitationUseCase;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.member.usecase.ChangeMemberSeatUseCase;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.usecase.CreateInvitationUseCase;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionSeatQuotasUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpgradeStarterSubscriptionUseCase;
import com.fabbitinc.server.application.subscription.usecase.command.UpgradeStarterSubscriptionCommand;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
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
    private AuthInvitationService authInvitationService;
    @Autowired
    private CreateInvitationUseCase createInvitationUseCase;
    @Autowired
    private AcceptInvitationUseCase acceptInvitationUseCase;
    @Autowired
    private ChangeMemberSeatUseCase changeMemberSeatUseCase;
    @Autowired
    private UpgradeStarterSubscriptionUseCase upgradeStarterSubscriptionUseCase;
    @Autowired
    private UpdateSubscriptionSeatQuotasUseCase updateSubscriptionSeatQuotasUseCase;
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

    @Test
    void team_워크스페이스_생성시_선택한_owner_좌석으로_초기_quota와_assignment가_생성된다() {
        var owner = fixture.createUser("team-viewer-owner@example.com");

        Organization organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM, SeatType.VIEWER);

        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow();
        var seatAssignment = subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(organization.getId(), owner.getId()).orElseThrow();
        var seatQuota = subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), SeatType.VIEWER).orElseThrow();

        assertEquals(WorkspacePlanType.TEAM, subscription.getPlanType());
        assertEquals(SeatType.VIEWER, seatAssignment.getSeatType());
        assertEquals(1, seatQuota.getPurchasedQuantity());
        assertEquals(WorkspacePlanType.TEAM.viewerMonthlyPrice(), seatQuota.getUnitPrice());
    }

    @Test
    void starter_업그레이드시_기존_멤버_좌석을_한번에_확정해_paid_플랜으로_전환한다() {
        var owner = fixture.createUser("starter-upgrade-owner@example.com");
        var collaborator = fixture.createUser("starter-upgrade-collab@example.com");
        var viewer = fixture.createUser("starter-upgrade-viewer@example.com");
        Organization organization = fixture.createWorkspace(owner, WorkspacePlanType.STARTER);
        var collaboratorMembership = fixture.addMember(collaborator, organization, MembershipRole.MEMBER);
        var viewerMembership = fixture.addMember(viewer, organization, MembershipRole.MEMBER);
        var ownerMembership = organizationService.getMembershipOrThrow(owner.getId(), organization.getId());

        fixture.upgradeStarterWorkspace(
                organization,
                WorkspacePlanType.TEAM,
                java.util.List.of(
                        new UpgradeStarterSubscriptionCommand.MemberSeatCommand(ownerMembership.getId(), SeatType.FULL),
                        new UpgradeStarterSubscriptionCommand.MemberSeatCommand(collaboratorMembership.getId(), SeatType.COLLABORATOR),
                        new UpgradeStarterSubscriptionCommand.MemberSeatCommand(viewerMembership.getId(), SeatType.VIEWER)
                ),
                owner,
                MembershipRole.OWNER
        );

        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow();
        var ownerSeat = subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(organization.getId(), owner.getId()).orElseThrow();
        var collaboratorSeat = subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(organization.getId(), collaborator.getId()).orElseThrow();
        var viewerSeat = subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(organization.getId(), viewer.getId()).orElseThrow();

        assertEquals(WorkspacePlanType.TEAM, subscription.getPlanType());
        assertEquals(SeatType.FULL, ownerSeat.getSeatType());
        assertEquals(SeatType.COLLABORATOR, collaboratorSeat.getSeatType());
        assertEquals(SeatType.VIEWER, viewerSeat.getSeatType());
        assertEquals(1, subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), SeatType.FULL).orElseThrow().getPurchasedQuantity());
        assertEquals(1, subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), SeatType.COLLABORATOR).orElseThrow().getPurchasedQuantity());
        assertEquals(1, subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), SeatType.VIEWER).orElseThrow().getPurchasedQuantity());
    }

    @Test
    void 유료_플랜_초대는_pending_상태에서도_좌석을_예약해_추가_초대를_막는다() {
        var owner = fixture.createUser("seat-reserve-owner@example.com");
        Organization organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM, SeatType.FULL);
        fixture.updateSeatQuota(organization, SeatType.VIEWER, 1, owner, MembershipRole.OWNER);

        fixture.createInvitation(
                organization,
                "viewer-invite-1@example.com",
                MembershipRole.MEMBER,
                SeatType.VIEWER,
                owner,
                MembershipRole.OWNER
        );

        AppException exception = assertThrows(AppException.class, () -> fixture.createInvitation(
                organization,
                "viewer-invite-2@example.com",
                MembershipRole.MEMBER,
                SeatType.VIEWER,
                owner,
                MembershipRole.OWNER
        ));

        assertEquals(ErrorCode.QUOTA_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void 유료_플랜_초대는_수락시_예약한_좌석으로_멤버를_활성화한다() {
        var owner = fixture.createUser("accept-invite-owner@example.com");
        Organization organization = fixture.createWorkspace(owner, WorkspacePlanType.TEAM, SeatType.FULL);
        fixture.updateSeatQuota(organization, SeatType.COLLABORATOR, 1, owner, MembershipRole.OWNER);
        var createdInvitation = fixture.createPendingInvitationRecord(
                organization,
                "accept-invite-member@example.com",
                MembershipRole.MEMBER,
                SeatType.COLLABORATOR,
                owner,
                MembershipRole.OWNER
        );

        fixture.acceptInvitation(createdInvitation.rawToken(), "Password123!", "협업 사용자");

        var invitedUser = userRepository.findByEmail("accept-invite-member@example.com").orElseThrow();
        var seatAssignment = subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(organization.getId(), invitedUser.getId()).orElseThrow();
        var membership = organizationService.getMembershipOrThrow(invitedUser.getId(), organization.getId());

        assertEquals(MembershipRole.MEMBER, membership.getRole());
        assertEquals(SeatType.COLLABORATOR, seatAssignment.getSeatType());
    }

    @Test
    void starter_업그레이드는_대기중인_초대가_있으면_실패한다() {
        var owner = fixture.createUser("starter-upgrade-pending-owner@example.com");
        Organization organization = fixture.createWorkspace(owner, WorkspacePlanType.STARTER);
        var ownerMembership = organizationService.getMembershipOrThrow(owner.getId(), organization.getId());
        fixture.createPendingInvitationRecord(
                organization,
                "pending-starter-invite@example.com",
                MembershipRole.MEMBER,
                SeatType.STARTER,
                owner,
                MembershipRole.OWNER
        );

        AppException exception = assertThrows(AppException.class, () -> fixture.upgradeStarterWorkspace(
                organization,
                WorkspacePlanType.TEAM,
                java.util.List.of(
                        new UpgradeStarterSubscriptionCommand.MemberSeatCommand(ownerMembership.getId(), SeatType.FULL)
                ),
                owner,
                MembershipRole.OWNER
        ));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }
}
