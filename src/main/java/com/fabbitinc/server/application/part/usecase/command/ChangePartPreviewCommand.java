package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import java.util.UUID;

public record ChangePartPreviewCommand(
        String partNumber,
        String revisionCode,
        String baseRevisionCode,
        String draftKey,
        PartPreviewSourceType sourceType,
        UUID sourceId
) {
}
