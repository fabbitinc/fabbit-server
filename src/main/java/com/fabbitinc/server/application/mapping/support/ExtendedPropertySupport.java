package com.fabbitinc.server.application.mapping.support;

public final class ExtendedPropertySupport {

    private static final String EXT_PREFIX = "_ext_";
    private static final String UNKNOWN_EXT_PROPERTY = "_ext_unknown";

    private ExtendedPropertySupport() {
    }

    public static boolean isExtendedProperty(String value) {
        return value != null && value.startsWith(EXT_PREFIX);
    }

    public static String normalizeExtendedProperty(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        while (normalized.startsWith("_ext__ext_")) {
            normalized = normalized.substring(EXT_PREFIX.length());
        }

        String core = normalized.startsWith(EXT_PREFIX)
                ? normalized.substring(EXT_PREFIX.length())
                : normalized;
        core = stripUnderscore(core);
        if (core.isBlank()) {
            return UNKNOWN_EXT_PROPERTY;
        }
        return EXT_PREFIX + core;
    }

    public static String normalizeSuggestedExtendedProperty(String rawSuggestion, String targetProperty) {
        String candidate = rawSuggestion;
        if (candidate == null || candidate.isBlank()) {
            candidate = defaultSuggestedExtendedProperty(targetProperty);
        }
        return normalizeExtendedProperty(candidate);
    }

    private static String defaultSuggestedExtendedProperty(String targetProperty) {
        String normalizedTarget = targetProperty == null ? "" : targetProperty.trim();
        if (normalizedTarget.isBlank()) {
            return UNKNOWN_EXT_PROPERTY;
        }
        if (isExtendedProperty(normalizedTarget)) {
            return normalizedTarget;
        }
        return EXT_PREFIX + normalizedTarget;
    }

    private static String stripUnderscore(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '_') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '_') {
            end--;
        }
        return value.substring(start, end);
    }
}
