package com.fabbitinc.server.application.mapping.usecase.command;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import java.util.UUID;

public record UpdateMappingCommand(
        UUID mappingId,
        UUID fileId,
        String name,
        String sheetName,
        MappingResultDto mapping
) {
}
