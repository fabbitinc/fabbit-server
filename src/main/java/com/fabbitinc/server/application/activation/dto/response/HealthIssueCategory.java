package com.fabbitinc.server.application.activation.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum HealthIssueCategory {
    EMPTY_GRAPH("empty_graph"),
    ORPHAN_PARTS("orphan_parts"),
    MISSING_DRAWING("missing_drawing"),
    MISSING_SUPPLIER("missing_supplier"),
    INCOMPLETE_BOM("incomplete_bom");

    private final String value;

    HealthIssueCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static HealthIssueCategory from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("health issue category는 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (HealthIssueCategory candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 health issue category입니다: " + raw);
    }
}
