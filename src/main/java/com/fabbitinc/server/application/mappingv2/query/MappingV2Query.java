package com.fabbitinc.server.application.mappingv2.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.query.condition.MappingV2DetailCondition;
import com.fabbitinc.server.application.mappingv2.query.condition.MappingV2ListCondition;
import com.fabbitinc.server.application.mappingv2.query.result.MappingV2ListResult;
import com.fabbitinc.server.application.mappingv2.query.result.MappingV2Result;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RecordRepository;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RevisionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MappingV2Query {

    private final MappingV2RecordRepository mappingV2RecordRepository;
    private final MappingV2RevisionRepository mappingV2RevisionRepository;
    private final ObjectMapper objectMapper;

    public MappingV2ListResult list(MappingV2ListCondition condition) {
        List<MappingV2Result> items = new ArrayList<>();
        for (MappingV2Record record : mappingV2RecordRepository.findByActiveTrueOrderByCreatedAtDesc()) {
            MappingV2Revision revision = mappingV2RevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                    .orElse(null);
            if (revision == null) {
                continue;
            }
            items.add(toMappingResult(record, revision));
        }
        return new MappingV2ListResult(items);
    }

    public MappingV2Result get(MappingV2DetailCondition condition) {
        MappingV2Record record = mappingV2RecordRepository.findById(condition.mappingId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑을 찾을 수 없습니다"));
        MappingV2Revision revision = mappingV2RevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑 리비전을 찾을 수 없습니다"));
        return toMappingResult(record, revision);
    }

    private MappingV2Result toMappingResult(MappingV2Record record, MappingV2Revision revision) {
        List<String> originalHeaders = parseOriginalHeaders(revision.getOriginalHeaders());
        MappingV2ResultDto mapping = parseMapping(revision.getMapping());

        List<String> mappedHeaders = new ArrayList<>(mapping.requiredColumns());
        mappedHeaders.sort(Comparator.comparingInt(header -> {
            int index = originalHeaders.indexOf(header);
            return index >= 0 ? index : Integer.MAX_VALUE;
        }));

        return new MappingV2Result(
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
