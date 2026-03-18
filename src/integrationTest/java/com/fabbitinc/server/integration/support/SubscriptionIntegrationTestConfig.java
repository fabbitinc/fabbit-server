package com.fabbitinc.server.integration.support;

import com.fabbitinc.server.application.auth.port.AuthEmailPort;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.organization.port.TenantProvisioningPort;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SubscriptionIntegrationTestConfig {

    @Bean
    public TestCurrentAuthProvider testCurrentAuthProvider() {
        return new TestCurrentAuthProvider();
    }

    @Bean
    @Primary
    public CurrentAuthProvider currentAuthProvider(TestCurrentAuthProvider provider) {
        return provider;
    }

    @Bean
    @Primary
    public AuthEmailPort authEmailPort() {
        return new TestAuthEmailPort();
    }

    @Bean
    @Primary
    public TenantProvisioningPort tenantProvisioningPort(DataSource dataSource) {
        return new TestTenantProvisioningPort(dataSource);
    }
}
