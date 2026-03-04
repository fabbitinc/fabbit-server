package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.request.MappingUpdateRequest;
import com.fabbitinc.server.application.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.MappingNormalizationSupport;
import com.fabbitinc.server.application.mapping.support.MappingValidationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UpdateMappingUseCase {

    private final MappingService mappingService;
    private final MappingNormalizationSupport mappingNormalizationSupport;
    private final MappingValidationSupport mappingValidationSupport;

    @Transactional
    public MappingResponse execute(UUID mappingId, MappingUpdateRequest request) {
        var file = mappingService.getUploadedFileOrThrow(request.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = mappingService.loadHeadersAndRows(file, request.sheetName(), 30);

        MappingResultDto normalized = mappingNormalizationSupport.normalize(request.mapping());
        MappingValidationSupport.ValidationResult validation = mappingValidationSupport.validateAgainstRows(
                parsed.headers(),
                parsed.rows(),
                normalized
        );

        if (!validation.errors().isEmpty()) {
            String detail = validation.errors().stream()
                    .limit(3)
                    .map(issue -> issue.message())
                    .collect(Collectors.joining("; "));
            throw new AppException(ErrorCode.VALIDATION_ERROR, "매핑 검증에 실패했습니다: " + detail);
        }

        return mappingService.updateMapping(mappingId, request, normalized);
    }
}
