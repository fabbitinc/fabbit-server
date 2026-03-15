package com.fabbitinc.server.application.mappingv2.usecase;

import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.result.MappingImpactSummaryResult;
import com.fabbitinc.server.application.mapping.usecase.result.MappingValidationIssueResult;
import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.support.MappingV2NormalizationSupport;
import com.fabbitinc.server.application.mappingv2.support.MappingV2ValidationSupport;
import com.fabbitinc.server.application.mappingv2.usecase.command.ValidateMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.result.ValidatedMappingV2Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidateMappingV2UseCase {

    private final MappingService mappingService;
    private final MappingV2NormalizationSupport mappingV2NormalizationSupport;
    private final MappingV2ValidationSupport mappingV2ValidationSupport;

    public ValidatedMappingV2Result execute(ValidateMappingV2Command command) {
        var file = mappingService.getUploadedFileOrThrow(command.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = mappingService.loadHeadersAndRows(file, command.sheetName(), 30);

        MappingV2ResultDto normalized = mappingV2NormalizationSupport.normalize(command.mapping());
        MappingV2ValidationSupport.ValidationResult validation = mappingV2ValidationSupport.validateAgainstRows(
                parsed.headers(),
                parsed.rows(),
                normalized
        );

        return new ValidatedMappingV2Result(
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
