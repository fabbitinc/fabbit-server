package com.fabbitinc.server.application.mapping.support;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MappingResponseMapper {

    private final ObjectMapper objectMapper;

    public MappingResponse toResponse(MappingRecord record, MappingRevision revision) {
        List<String> originalHeaders = parseOriginalHeaders(revision.getOriginalHeaders());
        MappingResultDto mapping = parseMapping(revision.getMapping());

        List<String> mappedHeaders = new ArrayList<>(mapping.requiredColumns());
        mappedHeaders.sort(Comparator.comparingInt(header -> {
            int index = originalHeaders.indexOf(header);
            return index >= 0 ? index : Integer.MAX_VALUE;
        }));

        return new MappingResponse(
                record.getId(),
                revision.getFileId(),
                record.getName(),
                revision.getSheetName(),
                originalHeaders,
                mappedHeaders,
                mapping,
                record.getScope(),
                record.isActive(),
                record.getUsageCount(),
                revision.getVersion(),
                record.getCreatedAt()
        );
    }

    public String writeMapping(MappingResultDto mapping) {
        try {
            return objectMapper.writeValueAsString(mapping);
        } catch (JacksonException ex) {
            return "{}";
        }
    }

    public String writeHeaders(List<String> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JacksonException ex) {
            return "[]";
        }
    }

    private List<String> parseOriginalHeaders(String rawHeaders) {
        if (rawHeaders == null || rawHeaders.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawHeaders, new TypeReference<>() {
            });
        } catch (JacksonException ex) {
            return List.of();
        }
    }

    private MappingResultDto parseMapping(String rawMapping) {
        if (rawMapping == null || rawMapping.isBlank()) {
            return new MappingResultDto(List.of(), List.of());
        }
        try {
            return objectMapper.readValue(rawMapping, MappingResultDto.class);
        } catch (JacksonException ex) {
            return new MappingResultDto(List.of(), List.of());
        }
    }
}
