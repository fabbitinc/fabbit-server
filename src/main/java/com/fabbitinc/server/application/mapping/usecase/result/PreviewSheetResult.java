package com.fabbitinc.server.application.mapping.usecase.result;

import tools.jackson.databind.JsonNode;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;

import java.util.List;

public record PreviewSheetResult(
        String sheetName,
        List<String> headers,
        List<JsonNode> sampleRows,
        MappingResultDto mapping
) {
}
