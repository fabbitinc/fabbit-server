package com.fabbitinc.server.application.mapping.usecase.support;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.usecase.result.SavedMappingResult;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SavedMappingResultMapper {

    private final ObjectMapper objectMapper;

    public SavedMappingResult toResult(MappingRecord record, MappingRevision revision) {
        List<String> originalHeaders = parseOriginalHeaders(revision.getOriginalHeaders());
        MappingResultDto mapping = parseMapping(revision.getMapping());

        List<String> mappedHeaders = new ArrayList<>(mapping.requiredColumns());
        mappedHeaders.sort(Comparator.comparingInt(header -> {
            int index = originalHeaders.indexOf(header);
            return index >= 0 ? index : Integer.MAX_VALUE;
        }));

        return new SavedMappingResult(
                record.getId(),
                revision.getFileId(),
                record.getName(),
                revision.getSheetName(),
                originalHeaders,
                mappedHeaders,
                mapping,
                record.isActive(),
                record.getUsageCount(),
                revision.getVersion(),
                record.getCreatedAt()
        );
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
