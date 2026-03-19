package com.fabbitinc.server.application.mapping.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.service.input.CreateMappingInput;
import com.fabbitinc.server.application.mapping.service.input.UpdateMappingInput;
import com.fabbitinc.server.application.mapping.service.output.SavedMappingOutput;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MappingService {

    private final MappingRecordRepository mappingRecordRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final SpreadsheetParserSupport spreadsheetParserSupport;
    private final ObjectMapper objectMapper;

    public File getUploadedFileOrThrow(UUID fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "업로드를 찾을 수 없습니다"));

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new AppException(
                    ErrorCode.PRECONDITION_FAILED,
                    "업로드가 완료되지 않은 파일입니다. 먼저 업로드를 완료해주세요."
            );
        }
        return file;
    }

    public List<String> loadPreviewTargets(File file, String requestedSheetName) {
        if (requestedSheetName != null && !requestedSheetName.isBlank()) {
            return List.of(requestedSheetName);
        }

        byte[] content = storagePort.getObject(file.getFileKey());
        List<String> sheetNames = spreadsheetParserSupport.getSheetNames(content, file.getOriginalName());
        if (sheetNames.isEmpty()) {
            List<String> single = new ArrayList<>();
            single.add(null);
            return single;
        }
        return sheetNames;
    }

    public SpreadsheetParserSupport.ParsedSheet loadHeadersAndRows(
            File file,
            String requestedSheetName,
            int maxRows
    ) {
        byte[] content = storagePort.getObject(file.getFileKey());
        return spreadsheetParserSupport.parse(content, file.getOriginalName(), requestedSheetName, maxRows);
    }

    public SavedMappingOutput createMapping(CreateMappingInput input) {
        ensureNameNotExists(input.name());

        MappingRecord record = MappingRecord.create(input.name());
        MappingRevision revision = record.createRevision(
                input.fileId(),
                input.sheetName(),
                writeHeaders(input.originalHeaders()),
                writeMapping(input.mapping())
        );

        mappingRecordRepository.save(record);
        mappingRevisionRepository.save(revision);

        return new SavedMappingOutput(record, revision);
    }

    public SavedMappingOutput updateMapping(java.util.UUID mappingId, UpdateMappingInput input) {
        MappingRecord record = mappingRecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));

        if (!record.isActive()) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "비활성화된 매핑은 수정할 수 없습니다");
        }

        if (input.name() != null && !input.name().isBlank() && !input.name().equals(record.getName())) {
            ensureNameNotExists(input.name(), record.getId());
            record.rename(input.name());
        }

        MappingRevision revision = record.createRevision(
                input.fileId(),
                input.sheetName(),
                writeHeaders(input.originalHeaders()),
                writeMapping(input.mapping())
        );
        mappingRevisionRepository.save(revision);

        return new SavedMappingOutput(record, revision);
    }

    public void deactivateMapping(java.util.UUID mappingId) {
        MappingRecord record = mappingRecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));
        record.deactivate();
    }

    private void ensureNameNotExists(String name) {
        if (mappingRecordRepository.existsByName(name)) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 동일한 이름의 매핑이 존재합니다: '" + name + "'"
            );
        }
    }

    private void ensureNameNotExists(String name, java.util.UUID excludeId) {
        boolean duplicated = excludeId == null
                ? mappingRecordRepository.existsByName(name)
                : mappingRecordRepository.existsByNameAndIdNot(name, excludeId);
        if (duplicated) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 동일한 이름의 매핑이 존재합니다: '" + name + "'"
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
