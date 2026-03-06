package com.fabbitinc.server.application.mapping.usecase.command;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;

import java.util.UUID;

public record ConfirmMappingCommand(
        UUID fileId,
        String name,
        String sheetName,
        MappingResultDto mapping
) {
}
