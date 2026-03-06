package com.fabbitinc.server.application.activity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ActivityScope {
    ISSUE("issue"),
    CR("cr"),
    PROJECT("project"),
    ORGANIZATION("organization");

    private final String value;

    ActivityScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ActivityScope from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("activity scope는 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ActivityScope candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 activity scope입니다: " + raw);
    }

    public static ActivityScope fromAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("activity action은 비어 있을 수 없습니다");
        }
        int index = action.indexOf(':');
        if (index < 0) {
            throw new IllegalArgumentException("activity action 형식이 올바르지 않습니다: " + action);
        }
        return from(action.substring(0, index));
    }
}
