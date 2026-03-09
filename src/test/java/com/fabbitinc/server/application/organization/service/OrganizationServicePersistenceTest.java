package com.fabbitinc.server.application.organization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.ServerApplication;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.JpaAuditingConfig;
import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.service.SubscriptionService;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = OrganizationServicePersistenceTest.TestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:organization-service-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@Import({
        JpaAuditingConfig.class,
        OrganizationService.class,
        SubscriptionApi.class,
        SubscriptionService.class,
        OrganizationServicePersistenceTest.TestConfig.class
})
class OrganizationServicePersistenceTest {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private TenantProvisioningRecorder tenantProvisioningRecorder;

    @Test
    void createOrganization_신규_조직과_오너_멤버십을_함께_저장한다() {
        User user = userRepository.save(User.create("owner@example.com", "hashed-password", "Owner"));

        Organization organization = organizationService.createOrganization(
                user.getId(),
                new CreateOrganizationInput(
                        "Acme",
                        "acme",
                        "manufacturing",
                        "11-50",
                        PlanType.STARTER
                )
        );

        assertTrue(organizationRepository.findById(organization.getId()).isPresent());

        Membership membership = membershipRepository.findByUserIdAndOrgId(user.getId(), organization.getId())
                .orElseThrow();
        assertEquals(MembershipRole.OWNER, membership.getRole());
        assertEquals(organization.getId(), tenantProvisioningRecorder.provisionedOrgId());

        Subscription subscription = subscriptionRepository
                .findByOrgIdAndStatus(organization.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow();
        assertEquals(PlanType.STARTER.name(), subscription.getPlanType());
        assertEquals(PlanType.STARTER.maxMembers(), subscription.getMaxMembers());
        assertEquals(PlanType.STARTER.aiCredits(), subscription.getAiCreditsGranted());
        assertEquals((long) PlanType.STARTER.storageGb() * 1_000_000_000L, subscription.getStorageBytesLimit());
        assertTrue(subscription.getCurrentPeriodEnd().isAfter(subscription.getCurrentPeriodStart()));
    }

    @Test
    void createOrganization_이미_생성한_워크스페이스가_있는_사용자는_새_조직을_생성할_수_없다() {
        User user = userRepository.save(User.create("owner-existing@example.com", "hashed-password", "Owner"));

        organizationService.createOrganization(
                user.getId(),
                new CreateOrganizationInput(
                        "Acme",
                        "acme-existing",
                        "manufacturing",
                        "11-50",
                        PlanType.STARTER
                )
        );

        AppException exception = assertThrows(AppException.class, () -> organizationService.createOrganization(
                user.getId(),
                new CreateOrganizationInput(
                        "Beta",
                        "beta-existing",
                        "manufacturing",
                        "11-50",
                        PlanType.BUSINESS
                )
        ));

        assertEquals(ErrorCode.ALREADY_EXISTS, exception.getErrorCode());
        assertEquals("이미 생성한 워크스페이스가 있어 새 조직을 생성할 수 없습니다", exception.getMessage());
    }

    @Test
    void consumeStorage와_releaseStorage가_조직_사용량을_갱신한다() {
        User user = userRepository.save(User.create("owner-storage@example.com", "hashed-password", "Owner"));
        Organization organization = organizationRepository.save(Organization.create(
                "acme-storage",
                "Acme Storage",
                user.getId(),
                null,
                null,
                PlanType.STARTER,
                5,
                100,
                1_024L
        ));

        organizationService.consumeStorage(organization.getId(), 512L);
        organizationService.releaseStorage(organization.getId(), 128L);

        Organization reloaded = organizationRepository.findById(organization.getId()).orElseThrow();
        assertEquals(384L, reloaded.getStorageBytesUsed());
    }

    @Test
    void consumeStorage는_한도초과시_quotaExceeded를_발생시킨다() {
        User user = userRepository.save(User.create("owner-limit@example.com", "hashed-password", "Owner"));
        Organization organization = organizationRepository.save(Organization.create(
                "acme-limit",
                "Acme Limit",
                user.getId(),
                null,
                null,
                PlanType.STARTER,
                5,
                100,
                100L
        ));

        AppException exception = assertThrows(AppException.class, () -> organizationService.consumeStorage(organization.getId(), 101L));
        assertEquals(ErrorCode.QUOTA_EXCEEDED, exception.getErrorCode());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        TenantProvisioningRecorder tenantProvisioningRecorder() {
            return new TenantProvisioningRecorder();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @AutoConfigurationPackage(basePackageClasses = ServerApplication.class)
    static class TestApplication {
    }

    static class TenantProvisioningRecorder implements TenantProvisioningPort {

        private UUID provisionedOrgId;

        @Override
        public String provisionTenant(UUID orgId) {
            this.provisionedOrgId = orgId;
            return "tenant";
        }

        UUID provisionedOrgId() {
            return provisionedOrgId;
        }
    }
}
