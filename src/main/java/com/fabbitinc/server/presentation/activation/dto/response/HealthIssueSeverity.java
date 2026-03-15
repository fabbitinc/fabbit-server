package com.fabbitinc.server.presentation.activation.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum HealthIssueSeverity {
    INFO("info"),
    WARNING("warning");

    private final String value;

    HealthIssueSeverity(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static HealthIssueSeverity from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("health issue severity는 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (HealthIssueSeverity candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 health issue severity입니다: " + raw);
    }
}
