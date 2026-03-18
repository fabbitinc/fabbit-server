package com.fabbitinc.server.application.property.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaCondition;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaListCondition;
import com.fabbitinc.server.application.property.query.result.PropertyMetaListResult;
import com.fabbitinc.server.application.property.query.result.PropertyMetaResult;
import com.fabbitinc.server.application.property.query.result.PropertyOptionResult;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionItem;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.SystemPropertyOverride;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.repository.SystemPropertyOverrideRepository;
import com.fabbitinc.server.domain.property.support.SystemPropertyRegistry;
import com.fabbitinc.server.domain.property.support.SystemPropertySpec;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final SystemPropertyOverrideRepository systemPropertyOverrideRepository;

    public PropertyMetaListResult list(PropertyMetaListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        List<PropertyMetaResult> items = buildMeta(resolveOwnerType(condition.ownerType()), condition.includeInactive());
        return new PropertyMetaListResult(items.size(), items);
    }

    public PropertyMetaResult get(PropertyMetaCondition condition) {
        currentAuthProvider.getCurrentAuth();
        PropertyOwnerType ownerType = resolveOwnerType(condition.ownerType());
        return buildMeta(ownerType, condition.includeInactive()).stream()
                .filter(item -> item.propertyKey().equals(condition.propertyKey()))
                .findFirst()
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "속성 메타 '%s/%s'을(를) 찾을 수 없습니다".formatted(
                                ownerType,
                                condition.propertyKey()
                        )
                ));
    }

    private List<PropertyMetaResult> buildMeta(PropertyOwnerType ownerType, boolean includeInactive) {
        Map<String, SystemPropertyOverride> overridesByKey = loadOverrides(ownerType, includeInactive).stream()
                .collect(java.util.stream.Collectors.toMap(
                        SystemPropertyOverride::getPropertyKey,
                        override -> override,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        List<PropertyMetaResult> systemItems = SystemPropertyRegistry.listByOwnerType(ownerType).stream()
                .map(spec -> toSystemMeta(ownerType, spec, overridesByKey.get(spec.propertyKey())))
                .filter(item -> includeInactive || item.active())
                .toList();

        List<PropertyMetaResult> customItems = loadDefinitions(ownerType, includeInactive).stream()
                .map(this::toCustomMeta)
                .toList();

        return java.util.stream.Stream.concat(systemItems.stream(), customItems.stream())
                .sorted(Comparator
                        .comparingInt(PropertyMetaResult::displayOrder)
                        .thenComparing(PropertyMetaResult::displayName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(PropertyMetaResult::propertyKey))
                .toList();
    }

    private List<SystemPropertyOverride> loadOverrides(PropertyOwnerType ownerType, boolean includeInactive) {
        return systemPropertyOverrideRepository.findByOwnerTypeOrderByDisplayOrderAscPropertyKeyAsc(ownerType);
    }

    private List<PropertyDefinition> loadDefinitions(PropertyOwnerType ownerType, boolean includeInactive) {
        if (includeInactive) {
            return propertyDefinitionRepository.findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(ownerType);
        }
        return propertyDefinitionRepository.findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(ownerType);
    }

    private PropertyMetaResult toSystemMeta(
            PropertyOwnerType ownerType,
            SystemPropertySpec spec,
            SystemPropertyOverride override
    ) {
        boolean active = override == null || override.isActive();
        String displayName = override != null && override.getDisplayNameOverride() != null
                ? override.getDisplayNameOverride()
                : spec.displayName();
        int displayOrder = override != null && override.getDisplayOrder() != null
                ? override.getDisplayOrder()
                : spec.displayOrder();

        return new PropertyMetaResult(
                null,
                ownerType,
                spec.propertyKey(),
                true,
                spec.partSystemPropertyKind(),
                spec.activeConfigurable(),
                spec.columnName(),
                displayName,
                spec.description(),
                spec.valueType(),
                spec.optionMode(),
                toOptionResults(spec.options()),
                displayOrder,
                spec.required(),
                active
        );
    }

    private PropertyMetaResult toCustomMeta(PropertyDefinition definition) {
        return new PropertyMetaResult(
                definition.getId(),
                definition.getOwnerType(),
                definition.getId().toString(),
                false,
                null,
                true,
                null,
                definition.getDisplayName(),
                definition.getDescription(),
                definition.getValueType(),
                definition.getOptionMode(),
                toOptionResults(definition.getOptions()),
                definition.getDisplayOrder(),
                definition.isRequired(),
                definition.isActive()
        );
    }

    private List<PropertyOptionResult> toOptionResults(List<PropertyOptionItem> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .filter(Objects::nonNull)
                .map(option -> new PropertyOptionResult(
                        option.value(),
                        option.label(),
                        option.displayOrder(),
                        option.active()
                ))
                .toList();
    }

    private PropertyOwnerType resolveOwnerType(String rawOwnerType) {
        try {
            return PropertyOwnerType.valueOf(rawOwnerType);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 owner_type입니다: " + rawOwnerType);
        }
    }
}
