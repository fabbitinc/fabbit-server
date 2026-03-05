package com.fabbitinc.server.domain.notification.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationSourceIssueType {
    ISSUE("issue"),
    CHANGE_REQUEST("change_request");

    private final String value;

    NotificationSourceIssueType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static NotificationSourceIssueType from(String rawValue) {
        for (NotificationSourceIssueType type : values()) {
            if (type.value.equals(rawValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 source_issue_type 입니다: " + rawValue);
    }
}
