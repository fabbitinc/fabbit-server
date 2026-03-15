package com.fabbitinc.server.application.mappingv2.query.result;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MappingV2Result(
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
    public MappingV2Result {
        originalHeaders = originalHeaders == null ? List.of() : List.copyOf(originalHeaders);
        mappedHeaders = mappedHeaders == null ? List.of() : List.copyOf(mappedHeaders);
        mapping = mapping == null ? new MappingV2ResultDto(List.of(), List.of()) : mapping;
    }
}
