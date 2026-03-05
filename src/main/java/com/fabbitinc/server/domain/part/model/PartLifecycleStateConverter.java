package com.fabbitinc.server.domain.part.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PartLifecycleStateConverter implements AttributeConverter<PartLifecycleState, String> {

    @Override
    public String convertToDatabaseColumn(PartLifecycleState attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.value();
    }

    @Override
    public PartLifecycleState convertToEntityAttribute(String dbData) {
        return PartLifecycleState.from(dbData);
    }
}
