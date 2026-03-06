package com.fabbitinc.server.application.mapping.query.result;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;

public record PropertyMappingResult(
        String sourceColumn,
        String targetProperty,
        PropertyDataType dataType,
        Integer confidence,
        String reason,
        Boolean isExtended
) {
    public PropertyMappingResult {
        dataType = dataType == null ? PropertyDataType.STRING : dataType;
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
        isExtended = isExtended != null && isExtended;
    }
}
