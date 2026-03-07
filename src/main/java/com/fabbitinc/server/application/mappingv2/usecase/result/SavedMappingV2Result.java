package com.fabbitinc.server.application.mappingv2.usecase.result;

import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SavedMappingV2Result(
        UUID id,
        UUID fileId,
        String name,
        String sheetName,
        List<String> originalHeaders,
        List<String> mappedHeaders,
        MappingV2ResultDto mapping,
        boolean active,
        int usageCount,
        int version,
        Instant createdAt
) {
}
