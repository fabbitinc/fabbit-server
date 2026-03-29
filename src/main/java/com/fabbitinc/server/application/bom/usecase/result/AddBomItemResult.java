package com.fabbitinc.server.application.bom.usecase.result;

import java.util.UUID;

public record AddBomItemResult(
        UUID partId,
        UUID revisionId,
        UUID bomItemId
) {
}
