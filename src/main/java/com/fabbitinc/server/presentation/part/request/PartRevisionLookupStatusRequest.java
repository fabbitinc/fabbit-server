package com.fabbitinc.server.presentation.part.request;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;

public enum PartRevisionLookupStatusRequest {
    DRAFT,
    RELEASED,
    SUPERSEDED,
    CANCELED;

    public PartRevisionStatus toDomainStatus() {
        return PartRevisionStatus.valueOf(name());
    }
}
