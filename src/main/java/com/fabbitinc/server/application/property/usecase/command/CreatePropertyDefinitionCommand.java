package com.fabbitinc.server.application.property.usecase.command;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.List;

public record CreatePropertyDefinitionCommand(
        String ownerType,
        String displayName,
        String description,
        PropertyValueType valueType,
        PropertyOptionMode optionMode,
        List<PropertyOptionCommandItem> options,
        int displayOrder,
        boolean required
) {
}
