package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.request.MappingValidateRequest;
import com.fabbitinc.server.application.mapping.dto.response.MappingValidateResponse;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.MappingNormalizationSupport;
import com.fabbitinc.server.application.mapping.support.MappingValidationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ValidateMappingUseCase {

    private final MappingService mappingService;
    private final MappingNormalizationSupport mappingNormalizationSupport;
    private final MappingValidationSupport mappingValidationSupport;

    @Transactional(readOnly = true)
    public MappingValidateResponse execute(MappingValidateRequest request) {
        var file = mappingService.getUploadedFileOrThrow(request.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = mappingService.loadHeadersAndRows(file, request.sheetName(), 30);

        MappingResultDto normalized = mappingNormalizationSupport.normalize(request.mapping());
        MappingValidationSupport.ValidationResult validation = mappingValidationSupport.validateAgainstRows(
                parsed.headers(),
                parsed.rows(),
                normalized
        );

        return new MappingValidateResponse(
                normalized,
                validation.errors(),
                validation.warnings(),
                validation.impactSummary()
        );
    }
}
