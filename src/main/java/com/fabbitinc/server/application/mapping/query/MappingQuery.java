package com.fabbitinc.server.application.mapping.query;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.dto.response.MappingListResponse;
import com.fabbitinc.server.application.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.application.mapping.support.MappingResponseMapper;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MappingQuery {

    private final MappingRecordRepository mappingRecordRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final MappingResponseMapper mappingResponseMapper;

    @Transactional(readOnly = true)
    public MappingListResponse listMappings() {
        List<MappingResponse> items = new ArrayList<>();
        for (MappingRecord record : mappingRecordRepository.findByActiveTrueOrderByCreatedAtDesc()) {
            MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                    .orElse(null);
            if (revision == null) {
                continue;
            }
            items.add(mappingResponseMapper.toResponse(record, revision));
        }
        return new MappingListResponse(items);
    }

    @Transactional(readOnly = true)
    public MappingResponse getMapping(UUID mappingId) {
        MappingRecord record = mappingRecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));

        MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(record.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑 리비전을 찾을 수 없습니다"));

        return mappingResponseMapper.toResponse(record, revision);
    }
}
