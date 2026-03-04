package com.fabbitinc.server.infrastructure.persistence.config;

import com.fabbitinc.server.infrastructure.persistence.tenant.CurrentTenantSchemaIdentifierResolver;
import com.fabbitinc.server.infrastructure.persistence.tenant.SchemaBasedMultiTenantConnectionProvider;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateMultiTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            SchemaBasedMultiTenantConnectionProvider connectionProvider,
            CurrentTenantSchemaIdentifierResolver tenantIdentifierResolver
    ) {
        return properties -> {
            properties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        };
    }
}
