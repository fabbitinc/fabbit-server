package com.fabbitinc.server.application.activation.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ActivationResultType {
    PART("part"),
    SUPPLIER("supplier"),
    PROJECT("project");

    private final String value;

    ActivationResultType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ActivationResultType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("activation result type은 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ActivationResultType candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 activation result type입니다: " + raw);
    }
}
