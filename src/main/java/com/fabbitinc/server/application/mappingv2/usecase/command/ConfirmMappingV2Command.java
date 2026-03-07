package com.fabbitinc.server.application.mappingv2.usecase.command;

import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import java.util.UUID;

public record ConfirmMappingV2Command(
        UUID fileId,
        String name,
        String sheetName,
        MappingV2ResultDto mapping
) {
}
