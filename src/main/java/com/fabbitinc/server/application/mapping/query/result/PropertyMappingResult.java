package com.fabbitinc.server.application.mapping.query.result;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.mapping.support.ExtendedPropertySupport;

public record PropertyMappingResult(
        String sourceColumn,
        String targetProperty,
        String suggestedExtendedProperty,
        PropertyDataType dataType,
        Integer confidence,
        String reason,
        Boolean isExtended
) {
    public PropertyMappingResult {
        targetProperty = targetProperty == null ? null : targetProperty.trim();
        suggestedExtendedProperty = ExtendedPropertySupport.normalizeSuggestedExtendedProperty(
                suggestedExtendedProperty,
                targetProperty
        );
        dataType = dataType == null ? PropertyDataType.STRING : dataType;
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
        isExtended = isExtended != null ? isExtended : ExtendedPropertySupport.isExtendedProperty(targetProperty);
    }
}
