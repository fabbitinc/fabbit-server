package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import java.util.UUID;

public record ChangePartPreviewCommand(
        UUID partId,
        PartPreviewSourceType sourceType,
        UUID sourceId
) {
}
