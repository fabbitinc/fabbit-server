package com.fabbitinc.server.application.mapping.query.result;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MappingResult(
        UUID id,
        UUID fileId,
        String name,
        String sheetName,
        List<String> originalHeaders,
        List<String> mappedHeaders,
        MappingResultDto mapping,
        boolean active,
        int usageCount,
        int version,
        Instant createdAt
) {
    public MappingResult {
        originalHeaders = originalHeaders == null ? List.of() : List.copyOf(originalHeaders);
        mappedHeaders = mappedHeaders == null ? List.of() : List.copyOf(mappedHeaders);
        mapping = mapping == null ? new MappingResultDto(List.of(), List.of()) : mapping;
    }
}
