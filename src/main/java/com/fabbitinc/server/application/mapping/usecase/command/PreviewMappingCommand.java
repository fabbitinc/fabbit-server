package com.fabbitinc.server.application.mapping.usecase.command;

import java.util.UUID;

public record PreviewMappingCommand(
        UUID fileId,
        String sheetName
) {
}
