package com.fabbitinc.server.domain.subscription.model;

public enum AiBillingMode {
    INCLUDED_ONLY,
    METERED

    ;

    public boolean isMetered() {
        return this == METERED;
    }
}
