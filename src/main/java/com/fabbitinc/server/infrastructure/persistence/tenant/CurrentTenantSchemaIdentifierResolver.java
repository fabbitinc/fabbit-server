package com.fabbitinc.server.infrastructure.persistence.tenant;

import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantSchemaIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContextHolder.getCurrentSchema();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
