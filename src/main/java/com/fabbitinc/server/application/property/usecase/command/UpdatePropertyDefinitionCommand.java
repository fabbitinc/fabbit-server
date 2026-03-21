package com.fabbitinc.server.application.property.usecase.command;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.List;

public record UpdatePropertyDefinitionCommand(
        String ownerType,
        String propertyKey,
        String displayName,
        boolean displayNameSet,
        String description,
        boolean descriptionSet,
        PropertyValueType valueType,
        boolean valueTypeSet,
        PropertyOptionMode optionMode,
        boolean optionModeSet,
        List<PropertyOptionCommandItem> options,
        boolean optionsSet,
        Integer displayOrder,
        boolean displayOrderSet,
        Boolean required,
        boolean requiredSet,
        Boolean active,
        boolean activeSet
) {
}
