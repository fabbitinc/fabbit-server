package com.fabbitinc.server.application.tenant.support;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TenantSchemaPolicy {

    public static final String PUBLIC_SCHEMA = "public";
    private static final Pattern TENANT_SCHEMA_PATTERN = Pattern.compile("^tenant_[0-9a-f]{32}$");
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]*$");

    private TenantSchemaPolicy() {
    }

    public static String schemaNameForOrgId(UUID orgId) {
        return "tenant_" + orgId.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    public static UUID orgIdForSchemaName(String schemaName) {
        String normalized = normalizeSchemaName(schemaName);
        if (PUBLIC_SCHEMA.equals(normalized)) {
            throw new IllegalArgumentException("public 스키마에서는 조직 ID를 해석할 수 없습니다");
        }

        String hex = normalized.substring("tenant_".length());
        String uuidText = hex.substring(0, 8)
                + "-"
                + hex.substring(8, 12)
                + "-"
                + hex.substring(12, 16)
                + "-"
                + hex.substring(16, 20)
                + "-"
                + hex.substring(20);
        return UUID.fromString(uuidText);
    }

    public static String normalizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return PUBLIC_SCHEMA;
        }
        String normalized = schemaName.trim().toLowerCase(Locale.ROOT);
        if (PUBLIC_SCHEMA.equals(normalized)) {
            return PUBLIC_SCHEMA;
        }
        if (!TENANT_SCHEMA_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효하지 않은 테넌트 스키마입니다: " + schemaName);
        }
        return normalized;
    }

    public static String normalizeSqlIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("식별자는 비어 있을 수 없습니다");
        }
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        if (!SQL_IDENTIFIER_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효하지 않은 SQL 식별자입니다: " + identifier);
        }
        return normalized;
    }

    public static String quoteIdentifier(String identifier) {
        String normalized = normalizeSqlIdentifier(identifier);
        return "\"" + normalized + "\"";
    }
}
