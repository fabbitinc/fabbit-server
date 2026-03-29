package com.fabbitinc.server.application.bom.usecase.result;

import java.util.List;
import java.util.UUID;

public record AddBomItemsBatchResult(
        UUID partId,
        UUID revisionId,
        List<UUID> bomItemIds
) {
}
