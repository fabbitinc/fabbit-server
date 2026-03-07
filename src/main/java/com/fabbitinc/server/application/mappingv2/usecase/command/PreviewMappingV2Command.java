package com.fabbitinc.server.application.mappingv2.usecase.command;

import java.util.UUID;

public record PreviewMappingV2Command(
        UUID fileId,
        String sheetName
) {
}
