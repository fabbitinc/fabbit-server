package com.fabbitinc.server.application.issue.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum TimelineItemType {
    COMMENT("comment"),
    ACTIVITY("activity");

    private final String value;

    TimelineItemType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static TimelineItemType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("timeline item type은 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (TimelineItemType candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 timeline item type입니다: " + raw);
    }
}
