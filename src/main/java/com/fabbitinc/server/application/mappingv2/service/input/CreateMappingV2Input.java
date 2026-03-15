package com.fabbitinc.server.application.mappingv2.service.input;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import java.util.List;
import java.util.UUID;

public record CreateMappingV2Input(
        String name,
        UUID fileId,
        String sheetName,
        List<String> originalHeaders,
        MappingV2ResultDto mapping
) {
}
