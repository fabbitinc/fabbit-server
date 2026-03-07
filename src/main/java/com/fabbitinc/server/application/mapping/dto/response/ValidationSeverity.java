package com.fabbitinc.server.application.mapping.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ValidationSeverity {
    ERROR("error"),
    WARNING("warning");

    private final String value;

    ValidationSeverity(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ValidationSeverity from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("validation severity는 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "error" -> ERROR;
            case "warning" -> WARNING;
            default -> throw new IllegalArgumentException("지원하지 않는 validation severity입니다: " + raw);
        };
    }
}
