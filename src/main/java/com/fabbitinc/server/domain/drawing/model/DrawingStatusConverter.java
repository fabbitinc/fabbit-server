package com.fabbitinc.server.domain.drawing.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DrawingStatusConverter implements AttributeConverter<DrawingStatus, String> {

    @Override
    public String convertToDatabaseColumn(DrawingStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public DrawingStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return DrawingStatus.valueOf(dbData);
    }
}
