package com.fabbitinc.server.application.ontology.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum RelationshipType {
    CONSISTS_OF,
    DEFINED_BY,
    SUPPLIED_BY,
    HAS_ITEM;

    @JsonValue
    public String value() {
        return name();
    }

    @JsonCreator
    public static RelationshipType from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("relType은 필수입니다");
        }

        return RelationshipType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
