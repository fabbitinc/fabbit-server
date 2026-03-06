package com.fabbitinc.server.application.mapping.service.input;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;

import java.util.UUID;

public record UpdateMappingInput(
        String name,
        UUID fileId,
        String sheetName,
        MappingResultDto mapping
) {
}
