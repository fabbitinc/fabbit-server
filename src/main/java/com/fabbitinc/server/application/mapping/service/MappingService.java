package com.fabbitinc.server.application.mapping.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.request.MappingConfirmRequest;
import com.fabbitinc.server.application.mapping.dto.request.MappingUpdateRequest;
import com.fabbitinc.server.application.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.application.mapping.support.MappingResponseMapper;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.mapping.model.MappingScope;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MappingService {

    private final MappingRecordRepository mappingRecordRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final SpreadsheetParserSupport spreadsheetParserSupport;
    private final MappingResponseMapper mappingResponseMapper;

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

    public MappingResponse createMapping(MappingConfirmRequest request, MappingResultDto normalizedMapping) {
        ensureNameNotExists(request.name(), null);

        File file = getUploadedFileOrThrow(request.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = loadHeadersAndRows(file, request.sheetName(), 0);
        MappingScope scope = determineScope(normalizedMapping);

        MappingRecord record = MappingRecord.create(request.name(), scope);
        MappingRevision revision = record.createRevision(
                file.getId(),
                request.sheetName(),
                mappingResponseMapper.writeHeaders(parsed.headers()),
                mappingResponseMapper.writeMapping(normalizedMapping)
        );

        mappingRecordRepository.save(record);
        mappingRevisionRepository.save(revision);

        return mappingResponseMapper.toResponse(record, revision);
    }

    public MappingResponse updateMapping(UUID mappingId, MappingUpdateRequest request, MappingResultDto normalizedMapping) {
        MappingRecord record = mappingRecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));

        if (!record.isActive()) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "비활성화된 매핑은 수정할 수 없습니다");
        }

        if (request.name() != null && !request.name().isBlank() && !request.name().equals(record.getName())) {
            ensureNameNotExists(request.name(), record.getId());
            record.rename(request.name());
        }

        File file = getUploadedFileOrThrow(request.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = loadHeadersAndRows(file, request.sheetName(), 0);

        record.changeScope(determineScope(normalizedMapping));

        MappingRevision revision = record.createRevision(
                file.getId(),
                request.sheetName(),
                mappingResponseMapper.writeHeaders(parsed.headers()),
                mappingResponseMapper.writeMapping(normalizedMapping)
        );
        mappingRevisionRepository.save(revision);

        return mappingResponseMapper.toResponse(record, revision);
    }

    public void deactivateMapping(UUID mappingId) {
        MappingRecord record = mappingRecordRepository.findById(mappingId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑을 찾을 수 없습니다"));
        record.deactivate();
    }

    private void ensureNameNotExists(String name, UUID excludeId) {
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

    private MappingScope determineScope(MappingResultDto mapping) {
        if (mapping.relationMappings().isEmpty()) {
            return MappingScope.PART_LIST;
        }

        Map<String, Set<String>> mergeKeysByLabel = ManufacturingOntology.ONTOLOGY.nodeLabels().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ManufacturingOntology.NodeLabelDef::label,
                        nodeLabel -> new LinkedHashSet<>(nodeLabel.mergeKeys())
                ));

        for (var relation : mapping.relationMappings()) {
            Set<String> requiredKeys = mergeKeysByLabel.getOrDefault(relation.targetLabel(), Set.of());
            for (String mergeKey : requiredKeys) {
                String value = relation.nodeColumns().get(mergeKey);
                if (value == null || value.isBlank()) {
                    return MappingScope.ROOT_BOM;
                }
            }
        }
        return MappingScope.FULL_BOM;
    }
}
