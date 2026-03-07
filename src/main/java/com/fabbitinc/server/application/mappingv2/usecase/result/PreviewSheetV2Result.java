package com.fabbitinc.server.application.mappingv2.usecase.result;

import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record PreviewSheetV2Result(
        String sheetName,
        List<String> headers,
        List<JsonNode> sampleRows,
        MappingV2ResultDto mapping
) {
}
