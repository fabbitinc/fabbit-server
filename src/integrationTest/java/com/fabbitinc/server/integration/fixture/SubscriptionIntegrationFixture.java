package com.fabbitinc.server.integration.fixture;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import com.fabbitinc.server.integration.support.TestCurrentAuthProvider;
import java.util.UUID;

public class SubscriptionIntegrationFixture {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final SubscriptionApi subscriptionApi;
    private final AiUsageService aiUsageService;
    private final TestCurrentAuthProvider testCurrentAuthProvider;

    public SubscriptionIntegrationFixture(
            UserRepository userRepository,
            OrganizationService organizationService,
            SubscriptionApi subscriptionApi,
            AiUsageService aiUsageService,
            TestCurrentAuthProvider testCurrentAuthProvider
    ) {
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.subscriptionApi = subscriptionApi;
        this.aiUsageService = aiUsageService;
        this.testCurrentAuthProvider = testCurrentAuthProvider;
    }

    public User createUser(String email) {
        return userRepository.save(User.create(email, "hashed-password", email.substring(0, email.indexOf('@'))));
    }

    public Organization createWorkspace(User owner, WorkspacePlanType planType) {
        String uniqueSlug = "org-" + owner.getId().toString().replace("-", "");
        return organizationService.createWorkspace(
                owner.getId(),
                new CreateOrganizationInput(
                        uniqueSlug,
                        uniqueSlug,
                        "manufacturing",
                        "11-50",
                        planType
                )
        );
    }

    public Membership addMember(User user, Organization organization, MembershipRole role) {
        return organizationService.addMember(user.getId(), organization.getId(), role);
    }

    public void assignSeat(Organization organization, User user, SeatType seatType, User actor, MembershipRole actorRole) {
        testCurrentAuthProvider.set(new AuthContext(actor.getId(), actor.getEmail(), organization.getId(), actorRole));
        Membership membership = organizationService.getMembershipOrThrow(user.getId(), organization.getId());
        subscriptionApi.changeSeatType(organization.getId(), membership, seatType, actor.getId());
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
}
