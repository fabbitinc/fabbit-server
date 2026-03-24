package com.fabbitinc.server.integration.fixture;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.usecase.AcceptInvitationUseCase;
import com.fabbitinc.server.application.auth.usecase.command.AcceptInvitationCommand;
import com.fabbitinc.server.application.member.usecase.ChangeMemberSeatUseCase;
import com.fabbitinc.server.application.member.usecase.command.ChangeMemberSeatCommand;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.organization.usecase.CreateInvitationUseCase;
import com.fabbitinc.server.application.organization.usecase.command.CreateInvitationCommand;
import com.fabbitinc.server.application.organization.usecase.result.CreateInvitationResult;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionSeatQuotasUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpgradeStarterSubscriptionUseCase;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionSeatQuotasCommand;
import com.fabbitinc.server.application.subscription.usecase.command.UpgradeStarterSubscriptionCommand;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatQuotaRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.support.TestCurrentAuthProvider;
import java.util.EnumMap;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SubscriptionIntegrationFixture {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final SubscriptionApi subscriptionApi;
    private final AiUsageService aiUsageService;
    private final AuthInvitationService authInvitationService;
    private final CreateInvitationUseCase createInvitationUseCase;
    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSeatQuotaRepository subscriptionSeatQuotaRepository;
    private final ChangeMemberSeatUseCase changeMemberSeatUseCase;
    private final UpgradeStarterSubscriptionUseCase upgradeStarterSubscriptionUseCase;
    private final UpdateSubscriptionSeatQuotasUseCase updateSubscriptionSeatQuotasUseCase;
    private final TestCurrentAuthProvider testCurrentAuthProvider;

    public SubscriptionIntegrationFixture(
            UserRepository userRepository,
            OrganizationService organizationService,
            SubscriptionApi subscriptionApi,
            AiUsageService aiUsageService,
            AuthInvitationService authInvitationService,
            CreateInvitationUseCase createInvitationUseCase,
            AcceptInvitationUseCase acceptInvitationUseCase,
            SubscriptionRepository subscriptionRepository,
            SubscriptionSeatQuotaRepository subscriptionSeatQuotaRepository,
            ChangeMemberSeatUseCase changeMemberSeatUseCase,
            UpgradeStarterSubscriptionUseCase upgradeStarterSubscriptionUseCase,
            UpdateSubscriptionSeatQuotasUseCase updateSubscriptionSeatQuotasUseCase,
            TestCurrentAuthProvider testCurrentAuthProvider
    ) {
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.subscriptionApi = subscriptionApi;
        this.aiUsageService = aiUsageService;
        this.authInvitationService = authInvitationService;
        this.createInvitationUseCase = createInvitationUseCase;
        this.acceptInvitationUseCase = acceptInvitationUseCase;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionSeatQuotaRepository = subscriptionSeatQuotaRepository;
        this.changeMemberSeatUseCase = changeMemberSeatUseCase;
        this.upgradeStarterSubscriptionUseCase = upgradeStarterSubscriptionUseCase;
        this.updateSubscriptionSeatQuotasUseCase = updateSubscriptionSeatQuotasUseCase;
        this.testCurrentAuthProvider = testCurrentAuthProvider;
    }

    public User createUser(String email) {
        return userRepository.save(User.create(email, "hashed-password", email.substring(0, email.indexOf('@'))));
    }

    public Organization createWorkspace(User owner, WorkspacePlanType planType) {
        return createWorkspace(owner, planType, planType.isStarter() ? null : SeatType.FULL);
    }

    public Organization createWorkspace(User owner, WorkspacePlanType planType, SeatType ownerSeatType) {
        String uniqueSlug = "org-" + owner.getId().toString().replace("-", "");
        return organizationService.createWorkspace(
                owner.getId(),
                new CreateOrganizationInput(
                        uniqueSlug,
                        uniqueSlug,
                        "manufacturing",
                        "11-50",
                        planType,
                        ownerSeatType
                )
        );
    }

    public Membership addMember(User user, Organization organization, MembershipRole role) {
        return organizationService.addMember(user.getId(), organization.getId(), role);
    }

    public void assignSeat(Organization organization, User user, SeatType seatType, User actor, MembershipRole actorRole) {
        setAuth(actor, organization, actorRole);
        try {
            changeMemberSeatUseCase.execute(new ChangeMemberSeatCommand(user.getId(), seatType));
        } finally {
            clearAuth();
        }
    }

    public void updateSeatQuota(Organization organization, SeatType seatType, int quantity, User actor, MembershipRole actorRole) {
        var subscription = subscriptionRepository.findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow();
        EnumMap<SeatType, Integer> requestedQuantities = new EnumMap<>(SeatType.class);
        subscriptionSeatQuotaRepository.findBySubscriptionId(subscription.getId())
                .forEach(quota -> requestedQuantities.put(quota.getSeatType(), quota.getPurchasedQuantity()));
        requestedQuantities.put(seatType, quantity);

        setAuth(actor, organization, actorRole);
        try {
            updateSubscriptionSeatQuotasUseCase.execute(new UpdateSubscriptionSeatQuotasCommand(
                    requestedQuantities.entrySet().stream()
                            .filter(entry -> entry.getKey() != SeatType.STARTER)
                            .map(entry -> new UpdateSubscriptionSeatQuotasCommand.SeatQuantityCommand(entry.getKey(), entry.getValue()))
                            .toList()
            ));
        } finally {
            clearAuth();
        }
    }

    public void upgradeStarterWorkspace(
            Organization organization,
            WorkspacePlanType targetPlanType,
            java.util.List<UpgradeStarterSubscriptionCommand.MemberSeatCommand> memberSeats,
            User actor,
            MembershipRole actorRole
    ) {
        setAuth(actor, organization, actorRole);
        try {
            upgradeStarterSubscriptionUseCase.execute(new UpgradeStarterSubscriptionCommand(targetPlanType, memberSeats));
        } finally {
            clearAuth();
        }
    }

    public CreateInvitationResult createInvitation(
            Organization organization,
            String email,
            MembershipRole role,
            SeatType seatType,
            User actor,
            MembershipRole actorRole
    ) {
        setAuth(actor, organization, actorRole);
        try {
            return createInvitationUseCase.execute(new CreateInvitationCommand(email, role, seatType));
        } finally {
            clearAuth();
        }
    }

    public AuthInvitationService.CreatedInvitation createPendingInvitationRecord(
            Organization organization,
            String email,
            MembershipRole role,
            SeatType seatType,
            User actor,
            MembershipRole actorRole
    ) {
        return authInvitationService.createInvitationRecord(
                organization.getId(),
                email,
                actor.getId(),
                role,
                seatType,
                actorRole
        );
    }

    public void acceptInvitation(String token, String password, String fullName) {
        acceptInvitationUseCase.execute(new AcceptInvitationCommand(token, password, fullName));
    }

    public void recordAiUsage(Organization organization, User user, AiUsageCategory category) {
        runInTenantContext(organization.getId(), () -> aiUsageService.record(new RecordAiUsageInput(
                organization.getId(),
                user.getId(),
                category,
                "integration-test",
                "test-model",
                10,
                20
        )));
    }

    public void runInTenantContext(UUID orgId, Runnable task) {
        TenantContextHolder.setCurrentSchema(TenantSchemaPolicy.schemaNameForOrgId(orgId));
        try {
            task.run();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void setAuth(User actor, Organization organization, MembershipRole actorRole) {
        testCurrentAuthProvider.set(new AuthContext(actor.getId(), actor.getEmail(), organization.getId(), actorRole));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                actor.getEmail(),
                "N/A",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + actorRole.name()))
        ));
    }

    private void clearAuth() {
        testCurrentAuthProvider.clear();
        SecurityContextHolder.clearContext();
    }
}
