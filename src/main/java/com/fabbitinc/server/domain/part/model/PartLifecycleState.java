package com.fabbitinc.server.domain.part.model;

public enum PartLifecycleState {
    ACTIVE,
    EOL,
    OBSOLETE;

    public boolean canTransitionTo(PartLifecycleState target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case ACTIVE -> target == EOL || target == OBSOLETE;
            case EOL -> target == OBSOLETE;
            case OBSOLETE -> false;
        };
    }
}
