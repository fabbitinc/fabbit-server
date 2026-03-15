package com.fabbitinc.server.application.mapping.usecase.result;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import java.util.List;

public record ValidatedMappingResult(
        MappingResultDto normalizedMapping,
        List<MappingValidationIssueResult> errors,
        List<MappingValidationIssueResult> warnings,
        MappingImpactSummaryResult impactSummary
) {
}
