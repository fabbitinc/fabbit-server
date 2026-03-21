package com.fabbitinc.server.domain.property.model;

public enum PropertySourceType {
    SYSTEM,
    CUSTOM;

    public boolean isSystem() {
        return this == SYSTEM;
    }
}
