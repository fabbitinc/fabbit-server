package com.fabbitinc.server.domain.part.model;

import java.util.Locale;

public enum PartLifecycleState {
    DESIGN("design"),
    PROTOTYPE("prototype"),
    PRODUCTION("production"),
    EOL("eol"),
    OBSOLETE("obsolete");

    private final String value;

    PartLifecycleState(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static PartLifecycleState from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        for (PartLifecycleState state : values()) {
            if (state.value.equals(normalized)) {
                return state;
            }
        }
        return null;
    }
}
