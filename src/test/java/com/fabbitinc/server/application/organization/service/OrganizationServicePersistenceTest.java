package com.fabbitinc.server.application.organization.service;

import com.fabbitinc.server.application.config.JpaAuditingConfig;
import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.Server2Application;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @AutoConfigurationPackage(basePackageClasses = Server2Application.class)
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
