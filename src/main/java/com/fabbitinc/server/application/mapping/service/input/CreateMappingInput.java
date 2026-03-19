package com.fabbitinc.server.application.mapping.service.input;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import java.util.List;
import java.util.UUID;

public record CreateMappingInput(
        String name,
        UUID fileId,
        String sheetName,
        List<String> originalHeaders,
        MappingResultDto mapping
) {
}
