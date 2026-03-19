package com.fabbitinc.server.application.mapping.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.query.condition.MappingDetailCondition;
import com.fabbitinc.server.application.mapping.query.condition.MappingListCondition;
import com.fabbitinc.server.application.mapping.query.result.MappingListResult;
import com.fabbitinc.server.application.mapping.query.result.MappingResult;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
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
public class MappingQuery {

    private final MappingRecordRepository mappingRecordRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final ObjectMapper objectMapper;

    public MappingListResult list(MappingListCondition condition) {
        List<MappingResult> items = new ArrayList<>();
        for (MappingRecord record : mappingRecordRepository.findByActiveTrueOrderByCreatedAtDesc()) {
            MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                    .orElse(null);
            if (revision == null) {
                continue;
            }
            items.add(toMappingResult(record, revision));
        }
        return new MappingListResult(items);
    }

    public MappingResult get(MappingDetailCondition condition) {
        MappingRecord record = mappingRecordRepository.findById(condition.mappingId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));
        MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑 리비전을 찾을 수 없습니다"));
        return toMappingResult(record, revision);
    }

    private MappingResult toMappingResult(MappingRecord record, MappingRevision revision) {
        List<String> originalHeaders = parseOriginalHeaders(revision.getOriginalHeaders());
        MappingResultDto mapping = parseMapping(revision.getMapping());

        List<String> mappedHeaders = new ArrayList<>(mapping.requiredColumns());
        mappedHeaders.sort(Comparator.comparingInt(header -> {
            int index = originalHeaders.indexOf(header);
            return index >= 0 ? index : Integer.MAX_VALUE;
        }));

        return new MappingResult(
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
