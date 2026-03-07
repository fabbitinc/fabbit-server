package com.fabbitinc.server.application.mappingv2.usecase.result;

import com.fabbitinc.server.application.mapping.usecase.result.SkippedSheetResult;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record PreviewMappingV2Result(
        List<String> headers,
        List<JsonNode> sampleRows,
        MappingV2ResultDto mapping,
        List<PreviewSheetV2Result> sheets,
        List<SkippedSheetResult> skippedSheets
) {
}
