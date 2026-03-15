package com.fabbitinc.server.application.mapping.usecase.result;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record PreviewMappingResult(
        List<String> headers,
        List<JsonNode> sampleRows,
        MappingResultDto mapping,
        List<PreviewSheetResult> sheets,
        List<SkippedSheetResult> skippedSheets
) {
}
