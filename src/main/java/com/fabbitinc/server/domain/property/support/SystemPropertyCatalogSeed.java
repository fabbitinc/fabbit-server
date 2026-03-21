package com.fabbitinc.server.domain.property.support;

import com.fabbitinc.server.domain.property.model.PropertyOptionItem;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.List;

public record SystemPropertyCatalogSeed(
        PropertyOwnerType ownerType,
        String propertyKey,
        PartSystemPropertyKind partSystemPropertyKind,
        String displayName,
        String description,
        PropertyValueType valueType,
        PropertyOptionMode optionMode,
        List<PropertyOptionItem> options,
        String storageBinding,
        int displayOrder,
        boolean required,
        boolean activeConfigurable
) {
}
