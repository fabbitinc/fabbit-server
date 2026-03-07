package com.fabbitinc.server.application.usage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum StorageCategory {
    DRAWING("drawing"),
    ATTACHMENT("attachment"),
    OTHER("other");

    private final String value;

    StorageCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static StorageCategory from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("storage category는 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (StorageCategory candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 storage category입니다: " + raw);
    }

    public static StorageCategory fromOwnerType(String ownerType) {
        if ("drawing".equals(ownerType)) {
            return DRAWING;
        }
        if ("part".equals(ownerType) || "issue".equals(ownerType)) {
            return ATTACHMENT;
        }
        return OTHER;
    }
}
