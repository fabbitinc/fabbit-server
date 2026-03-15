package com.fabbitinc.server.application.mapping.usecase.command;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import java.util.UUID;

public record ConfirmMappingCommand(
        UUID fileId,
        String name,
        String sheetName,
        MappingResultDto mapping
) {
}
