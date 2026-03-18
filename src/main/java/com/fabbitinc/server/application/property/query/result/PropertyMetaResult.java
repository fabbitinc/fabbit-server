package com.fabbitinc.server.application.property.query.result;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import java.util.List;
import java.util.UUID;

public record PropertyMetaResult(
        UUID definitionId,
        PropertyOwnerType ownerType,
        String propertyKey,
        boolean system,
        PartSystemPropertyKind partSystemPropertyKind,
        String columnName,
        String displayName,
        String description,
        PropertyValueType valueType,
        PropertyOptionMode optionMode,
        List<PropertyOptionResult> options,
        int displayOrder,
        boolean required,
        boolean active
) {
}
