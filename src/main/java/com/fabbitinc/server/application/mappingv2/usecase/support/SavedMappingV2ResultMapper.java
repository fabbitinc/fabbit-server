package com.fabbitinc.server.application.mappingv2.usecase.support;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.usecase.result.SavedMappingV2Result;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
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
public class SavedMappingV2ResultMapper {

    private final ObjectMapper objectMapper;

    public SavedMappingV2Result toResult(MappingV2Record record, MappingV2Revision revision) {
        List<String> originalHeaders = parseOriginalHeaders(revision.getOriginalHeaders());
        MappingV2ResultDto mapping = parseMapping(revision.getMapping());

        List<String> mappedHeaders = new ArrayList<>(mapping.requiredColumns());
        mappedHeaders.sort(Comparator.comparingInt(header -> {
            int index = originalHeaders.indexOf(header);
            return index >= 0 ? index : Integer.MAX_VALUE;
        }));

        return new SavedMappingV2Result(
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

    private MappingV2ResultDto parseMapping(String rawMapping) {
        if (rawMapping == null || rawMapping.isBlank()) {
            return new MappingV2ResultDto(List.of(), List.of());
        }
        try {
            return objectMapper.readValue(rawMapping, MappingV2ResultDto.class);
        } catch (JacksonException ex) {
            return new MappingV2ResultDto(List.of(), List.of());
        }
    }
}
