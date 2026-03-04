package com.fabbitinc.server.application.usage.dto.response;

public enum StorageCategory {
    DRAWING("drawing"),
    ATTACHMENT("attachment"),
    OTHER("other");

    private final String value;

    StorageCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
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
