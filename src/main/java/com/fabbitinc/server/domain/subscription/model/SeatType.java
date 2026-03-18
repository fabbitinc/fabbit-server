package com.fabbitinc.server.domain.subscription.model;

public enum SeatType {
    STARTER,
    VIEWER,
    COLLABORATOR,
    FULL;

    public boolean isFullSeat() {
        return this == FULL;
    }
}
