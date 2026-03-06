package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.MappingNormalizationSupport;
import com.fabbitinc.server.application.mapping.support.MappingValidationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.command.ValidateMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.result.MappingImpactSummaryResult;
import com.fabbitinc.server.application.mapping.usecase.result.MappingValidationIssueResult;
import com.fabbitinc.server.application.mapping.usecase.result.ValidatedMappingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidateMappingUseCase {

    private final MappingService mappingService;
    private final MappingNormalizationSupport mappingNormalizationSupport;
    private final MappingValidationSupport mappingValidationSupport;

    public ValidatedMappingResult execute(ValidateMappingCommand command) {
        var file = mappingService.getUploadedFileOrThrow(command.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = mappingService.loadHeadersAndRows(file, command.sheetName(), 30);

        MappingResultDto normalized = mappingNormalizationSupport.normalize(command.mapping());
        MappingValidationSupport.ValidationResult validation = mappingValidationSupport.validateAgainstRows(
                parsed.headers(),
                parsed.rows(),
                normalized
        );

        return new ValidatedMappingResult(
                normalized,
                validation.errors().stream()
                        .map(issue -> new MappingValidationIssueResult(
                                issue.code(),
                                issue.severity(),
                                issue.message(),
                                issue.path(),
                                issue.dismissedReason()
                        ))
                        .toList(),
                validation.warnings().stream()
                        .map(issue -> new MappingValidationIssueResult(
                                issue.code(),
                                issue.severity(),
                                issue.message(),
                                issue.path(),
                                issue.dismissedReason()
                        ))
                        .toList(),
                new MappingImpactSummaryResult(validation.impactSummary().disabledColumnCount())
        );
    }
}
