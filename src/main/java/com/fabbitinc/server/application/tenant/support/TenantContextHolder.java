package com.fabbitinc.server.application.tenant.support;

public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setCurrentSchema(String schemaName) {
        CURRENT_SCHEMA.set(TenantSchemaPolicy.normalizeSchemaName(schemaName));
    }

    public static String getCurrentSchema() {
        String schemaName = CURRENT_SCHEMA.get();
        if (schemaName == null || schemaName.isBlank()) {
            return TenantSchemaPolicy.PUBLIC_SCHEMA;
        }
        return schemaName;
    }

    public static void clear() {
        CURRENT_SCHEMA.remove();
    }
}
