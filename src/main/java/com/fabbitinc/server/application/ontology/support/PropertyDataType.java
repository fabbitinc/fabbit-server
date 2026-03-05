package com.fabbitinc.server.application.ontology.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PropertyDataType {
    STRING("string"),
    INTEGER("integer"),
    FLOAT("float"),
    BOOLEAN("boolean");

    private final String value;

    PropertyDataType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PropertyDataType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("dataType은 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "string" -> STRING;
            case "integer" -> INTEGER;
            case "float" -> FLOAT;
            case "boolean" -> BOOLEAN;
            default -> throw new IllegalArgumentException("지원하지 않는 dataType입니다: " + raw);
        };
    }
}
