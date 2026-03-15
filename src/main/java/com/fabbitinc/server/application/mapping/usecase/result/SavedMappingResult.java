package com.fabbitinc.server.application.mapping.usecase.result;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.domain.mapping.model.MappingScope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SavedMappingResult(
        UUID id,
        UUID fileId,
        String name,
        String sheetName,
        List<String> originalHeaders,
        List<String> mappedHeaders,
        MappingResultDto mapping,
        MappingScope scope,
        boolean active,
        int usageCount,
        int version,
        Instant createdAt
) {
}
