package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import java.util.UUID;

public record EngineeringChangeAffectedItemResult(
        UUID id,
        EngineeringChangeAffectedItemType itemType,
        UUID targetId,
        String actionDetail,
        UUID partId,
        String partNumber,
        String revisionCode,
        String name,
        String status
) {
}
