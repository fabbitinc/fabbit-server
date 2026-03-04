package com.fabbitinc.server.application.organization.port;

import java.util.UUID;

public interface TenantProvisioningPort {

    String provisionTenant(UUID orgId);
}
