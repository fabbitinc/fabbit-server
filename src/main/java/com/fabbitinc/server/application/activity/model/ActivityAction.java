package com.fabbitinc.server.application.activity.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ActivityAction {
    ISSUE_CREATED("issue:created", ActivityScope.ISSUE),
    ISSUE_STATE_CHANGED("issue:state_changed", ActivityScope.ISSUE),
    ISSUE_ASSIGNEE_CHANGED("issue:assignee_changed", ActivityScope.ISSUE),
    ISSUE_REVIEWER_CHANGED("issue:reviewer_changed", ActivityScope.ISSUE),
    ISSUE_LABEL_CHANGED("issue:label_changed", ActivityScope.ISSUE),
    ISSUE_PART_CHANGED("issue:part_changed", ActivityScope.ISSUE),
    ISSUE_FILE_ATTACHED("issue:file_attached", ActivityScope.ISSUE),
    ISSUE_FILE_DETACHED("issue:file_detached", ActivityScope.ISSUE),
    ISSUE_ENGINEERING_CHANGE_CHANGED("issue:engineering_change_changed", ActivityScope.ISSUE),
    ISSUE_MENTIONED("issue:mentioned", ActivityScope.ISSUE),
    ENGINEERING_CHANGE_STATE_CHANGED("engineering_change:state_changed", ActivityScope.ENGINEERING_CHANGE),
    ENGINEERING_CHANGE_ISSUE_CHANGED("engineering_change:issue_changed", ActivityScope.ENGINEERING_CHANGE),
    ENGINEERING_CHANGE_STEP_CHANGED("engineering_change:step_changed", ActivityScope.ENGINEERING_CHANGE),
    ENGINEERING_CHANGE_FILE_ATTACHED("engineering_change:file_attached", ActivityScope.ENGINEERING_CHANGE),
    ENGINEERING_CHANGE_FILE_DETACHED("engineering_change:file_detached", ActivityScope.ENGINEERING_CHANGE),
    ENGINEERING_CHANGE_MENTIONED("engineering_change:mentioned", ActivityScope.ENGINEERING_CHANGE),
    ENGINEERING_CHANGE_PART_REVISION_CHANGED(
            "engineering_change:part_revision_changed",
            ActivityScope.ENGINEERING_CHANGE
    ),
    ENGINEERING_CHANGE_STEP_APPROVED(
            "engineering_change:step_approved",
            ActivityScope.ENGINEERING_CHANGE
    ),
    ENGINEERING_CHANGE_STEP_REJECTED(
            "engineering_change:step_rejected",
            ActivityScope.ENGINEERING_CHANGE
    ),
    ENGINEERING_CHANGE_STEP_CHANGES_REQUESTED(
            "engineering_change:step_changes_requested",
            ActivityScope.ENGINEERING_CHANGE
    ),
    ENGINEERING_CHANGE_STEP_RESUBMITTED(
            "engineering_change:step_resubmitted",
            ActivityScope.ENGINEERING_CHANGE
    );

    private final String value;
    private final ActivityScope scope;

    ActivityAction(String value, ActivityScope scope) {
        this.value = value;
        this.scope = scope;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public ActivityScope scope() {
        return scope;
    }

    @JsonCreator
    public static ActivityAction from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("activity action은 필수입니다");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ActivityAction candidate : values()) {
            if (candidate.value.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 activity action입니다: " + raw);
    }
}
