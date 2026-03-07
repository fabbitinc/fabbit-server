package com.fabbitinc.server.application.mappingv2.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mappingv2.service.input.CreateMappingV2Input;
import com.fabbitinc.server.application.mappingv2.service.input.UpdateMappingV2Input;
import com.fabbitinc.server.application.mappingv2.service.output.SavedMappingV2Output;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RecordRepository;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MappingV2Service {

    private final MappingV2RecordRepository mappingV2RecordRepository;
    private final MappingV2RevisionRepository mappingV2RevisionRepository;
    private final ObjectMapper objectMapper;

    public SavedMappingV2Output createMapping(CreateMappingV2Input input) {
        ensureNameNotExists(input.name());

        MappingV2Record record = MappingV2Record.create(input.name());
        MappingV2Revision revision = record.createRevision(
                input.fileId(),
                input.sheetName(),
                writeHeaders(input.originalHeaders()),
                writeMapping(input.mapping())
        );

        mappingV2RecordRepository.save(record);
        mappingV2RevisionRepository.save(revision);

        return new SavedMappingV2Output(record, revision);
    }

    public SavedMappingV2Output updateMapping(java.util.UUID mappingId, UpdateMappingV2Input input) {
        MappingV2Record record = mappingV2RecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑을 찾을 수 없습니다"));

        if (!record.isActive()) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "비활성화된 V2 매핑은 수정할 수 없습니다");
        }

        if (input.name() != null && !input.name().isBlank() && !input.name().equals(record.getName())) {
            ensureNameNotExists(input.name(), record.getId());
            record.rename(input.name());
        }

        MappingV2Revision revision = record.createRevision(
                input.fileId(),
                input.sheetName(),
                writeHeaders(input.originalHeaders()),
                writeMapping(input.mapping())
        );
        mappingV2RevisionRepository.save(revision);

        return new SavedMappingV2Output(record, revision);
    }

    public void deactivateMapping(java.util.UUID mappingId) {
        MappingV2Record record = mappingV2RecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑을 찾을 수 없습니다"));
        record.deactivate();
    }

    private void ensureNameNotExists(String name) {
        if (mappingV2RecordRepository.existsByName(name)) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 동일한 이름의 V2 매핑이 존재합니다: '" + name + "'"
            );
        }
    }

    private void ensureNameNotExists(String name, java.util.UUID excludeId) {
        boolean duplicated = excludeId == null
                ? mappingV2RecordRepository.existsByName(name)
                : mappingV2RecordRepository.existsByNameAndIdNot(name, excludeId);
        if (duplicated) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 동일한 이름의 V2 매핑이 존재합니다: '" + name + "'"
            );
        }
    }

    private String writeMapping(Object mapping) {
        try {
            return objectMapper.writeValueAsString(mapping);
        } catch (JacksonException ex) {
            return "{}";
        }
    }

    private String writeHeaders(Object headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JacksonException ex) {
            return "[]";
        }
    }
}
