package com.fabbitinc.server.application.mappingv2.usecase.result;

import com.fabbitinc.server.application.mapping.usecase.result.MappingImpactSummaryResult;
import com.fabbitinc.server.application.mapping.usecase.result.MappingValidationIssueResult;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import java.util.List;

public record ValidatedMappingV2Result(
        MappingV2ResultDto normalizedMapping,
        List<MappingValidationIssueResult> errors,
        List<MappingValidationIssueResult> warnings,
        MappingImpactSummaryResult impactSummary
) {
}
