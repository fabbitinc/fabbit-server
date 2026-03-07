package com.fabbitinc.server.application.mapping.usecase.result;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record PreviewSheetResult(
        String sheetName,
        List<String> headers,
        List<JsonNode> sampleRows,
        MappingResultDto mapping
) {
}
