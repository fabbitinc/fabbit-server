package com.fabbitinc.server.application.property.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.application.property.api.PropertyDefinitionUsageSummary;
import com.fabbitinc.server.application.property.usecase.command.PropertyOptionCommandItem;
import com.fabbitinc.server.application.property.usecase.command.ReorderPropertyCommandItem;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionItem;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.model.SystemPropertyOverride;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.repository.SystemPropertyOverrideRepository;
import com.fabbitinc.server.domain.property.support.SystemPropertyRegistry;
import com.fabbitinc.server.domain.property.support.SystemPropertySpec;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final SystemPropertyOverrideRepository systemPropertyOverrideRepository;
    private final PropertyApi propertyApi;

    public PropertyDefinition createCustomProperty(
            PropertyOwnerType ownerType,
            String displayName,
            String description,
            PropertyValueType valueType,
            PropertyOptionMode optionMode,
            List<PropertyOptionCommandItem> options,
            int displayOrder,
            boolean required
    ) {
        try {
            if (propertyDefinitionRepository.existsByOwnerTypeAndDisplayName(ownerType, displayName)) {
                throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 속성 표시명입니다: " + displayName);
            }

            PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                    ownerType,
                    displayName,
                    description,
                    valueType,
                    optionMode,
                    toOptionItems(options),
                    displayOrder,
                    required
            );
            return propertyDefinitionRepository.save(definition);
        } catch (DomainException ex) {
            throw toValidationException(ex);
        }
    }

    public PropertyDefinition updateCustomProperty(
            UUID propertyDefinitionId,
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
        try {
            PropertyDefinition definition = getRequiredDefinition(propertyDefinitionId);

            if (displayNameSet && propertyDefinitionRepository.existsByOwnerTypeAndDisplayNameAndIdNot(
                    definition.getOwnerType(),
                    displayName,
                    definition.getId()
            )) {
                throw new AppException(ErrorCode.CONFLICT, "이미 존재하는 속성 표시명입니다: " + displayName);
            }

            if (displayNameSet) {
                definition.renameDisplayName(displayName);
            }
            if (descriptionSet) {
                definition.changeDescription(description);
            }

            PropertyValueType nextValueType = valueTypeSet ? valueType : definition.getValueType();
            PropertyOptionMode nextOptionMode = optionModeSet ? optionMode : definition.getOptionMode();
            List<PropertyOptionItem> nextOptions = optionsSet ? toOptionItems(options) : definition.getOptions();
            if (valueTypeSet || optionModeSet || optionsSet) {
                definition.reconfigureValueSpec(nextValueType, nextOptionMode, nextOptions);
            }

            if (displayOrderSet) {
                definition.reorder(displayOrder == null ? 0 : displayOrder);
            }
            if (requiredSet) {
                if (Boolean.TRUE.equals(required)) {
                    definition.markRequired();
                } else {
                    definition.markOptional();
                }
            }
            if (activeSet) {
                if (Boolean.TRUE.equals(active)) {
                    definition.activate();
                } else {
                    definition.deactivate();
                }
            }
            return definition;
        } catch (DomainException ex) {
            throw toValidationException(ex);
        }
    }

    public void deleteCustomProperty(UUID propertyDefinitionId) {
        PropertyDefinition definition = getRequiredDefinition(propertyDefinitionId);
        PropertyDefinitionUsageSummary usageSummary = propertyApi.getPropertyDefinitionUsage(definition.getId());
        if (usageSummary.inUse()) {
            throw new AppException(
                    ErrorCode.CONFLICT,
                    "이미 사용 중인 속성은 삭제할 수 없습니다: %s (%s)".formatted(
                            definition.getDisplayName(),
                            usageSummary.describe()
                    )
            );
        }
        propertyDefinitionRepository.delete(definition);
    }

    public void reorderProperties(PropertyOwnerType ownerType, List<ReorderPropertyCommandItem> properties) {
        List<ReorderPropertyCommandItem> normalizedProperties = normalizeReorderProperties(properties);
        Map<String, SystemPropertySpec> systemSpecsByKey = new LinkedHashMap<>();
        List<UUID> customPropertyDefinitionIds = new java.util.ArrayList<>();

        for (ReorderPropertyCommandItem property : normalizedProperties) {
            String propertyKey = property.propertyKey();
            SystemPropertySpec spec = SystemPropertyRegistry.find(ownerType, propertyKey).orElse(null);
            if (spec != null) {
                if (!property.system()) {
                    throw new AppException(
                            ErrorCode.BAD_REQUEST,
                            "시스템 속성은 system=true로 보내야 합니다: " + propertyKey
                    );
                }
                systemSpecsByKey.put(propertyKey, spec);
                continue;
            }
            if (property.system()) {
                throw new AppException(
                        ErrorCode.BAD_REQUEST,
                        "커스텀 속성은 system=false로 보내야 합니다: " + propertyKey
                );
            }
            customPropertyDefinitionIds.add(parsePropertyDefinitionId(propertyKey));
        }

        Map<UUID, PropertyDefinition> customDefinitionsById = propertyDefinitionRepository
                .findByIdInAndOwnerType(customPropertyDefinitionIds, ownerType)
                .stream()
                .collect(java.util.stream.Collectors.toMap(PropertyDefinition::getId, definition -> definition));

        if (customDefinitionsById.size() != customPropertyDefinitionIds.size()) {
            Set<UUID> missingIds = customPropertyDefinitionIds.stream()
                    .filter(id -> !customDefinitionsById.containsKey(id))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "재정렬할 커스텀 속성 정의를 찾을 수 없습니다: " + missingIds
            );
        }

        Map<String, SystemPropertyOverride> overridesByKey = systemPropertyOverrideRepository
                .findByOwnerTypeOrderByDisplayOrderAscPropertyKeyAsc(ownerType)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        SystemPropertyOverride::getPropertyKey,
                        override -> override,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        List<Integer> reorderedDisplayOrders = normalizedProperties.stream()
                .map(property -> resolveCurrentDisplayOrder(
                        property.propertyKey(),
                        systemSpecsByKey,
                        customDefinitionsById,
                        overridesByKey
                ))
                .sorted()
                .toList();

        for (int index = 0; index < normalizedProperties.size(); index++) {
            String propertyKey = normalizedProperties.get(index).propertyKey();
            int nextDisplayOrder = reorderedDisplayOrders.get(index);
            SystemPropertySpec systemSpec = systemSpecsByKey.get(propertyKey);
            if (systemSpec != null) {
                applySystemPropertyDisplayOrder(
                        ownerType,
                        propertyKey,
                        systemSpec,
                        overridesByKey,
                        nextDisplayOrder
                );
                continue;
            }

            PropertyDefinition definition = customDefinitionsById.get(parsePropertyDefinitionId(propertyKey));
            if (definition.getDisplayOrder() != nextDisplayOrder) {
                definition.reorder(nextDisplayOrder);
            }
        }
    }

    public SystemPropertyOverride upsertSystemPropertyOverride(
            PropertyOwnerType ownerType,
            String propertyKey,
            String displayNameOverride,
            Integer displayOrder,
            Boolean active
    ) {
        try {
            SystemPropertySpec spec = SystemPropertyRegistry.find(ownerType, propertyKey)
                    .orElse(null);
            if (spec == null) {
                throw new AppException(
                        ErrorCode.BAD_REQUEST,
                        "시스템 속성 '%s/%s'은(는) 존재하지 않습니다".formatted(ownerType, propertyKey)
                );
            }
            if (Boolean.FALSE.equals(active) && !spec.activeConfigurable()) {
                throw new AppException(
                        ErrorCode.VALIDATION_ERROR,
                        "시스템 속성 '%s/%s'은(는) 비활성화할 수 없습니다".formatted(ownerType, propertyKey)
                );
            }

            SystemPropertyOverride override = systemPropertyOverrideRepository
                    .findByOwnerTypeAndPropertyKey(ownerType, propertyKey)
                    .orElseGet(() -> SystemPropertyOverride.create(
                            ownerType,
                            propertyKey,
                            null,
                            null,
                            true
                    ));

            override.changeDisplayNameOverride(displayNameOverride);
            override.changeDisplayOrder(displayOrder);
            if (active == null || active) {
                override.activate();
            } else {
                override.deactivate();
            }
            return systemPropertyOverrideRepository.save(override);
        } catch (DomainException ex) {
            throw toValidationException(ex);
        }
    }

    private PropertyDefinition getRequiredDefinition(UUID propertyDefinitionId) {
        return propertyDefinitionRepository.findById(propertyDefinitionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "속성 정의 '%s'을(를) 찾을 수 없습니다".formatted(propertyDefinitionId)
                ));
    }

    private AppException toValidationException(DomainException ex) {
        return new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    }

    private List<ReorderPropertyCommandItem> normalizeReorderProperties(List<ReorderPropertyCommandItem> properties) {
        if (properties == null || properties.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "properties는 최소 1개 이상이어야 합니다");
        }

        List<ReorderPropertyCommandItem> normalized = new java.util.ArrayList<>(properties.size());
        Set<String> seen = new LinkedHashSet<>();
        for (ReorderPropertyCommandItem property : properties) {
            if (property == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "properties에 null 값이 포함될 수 없습니다");
            }
            String propertyKey = property.propertyKey();
            if (propertyKey == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "property_key는 비어 있을 수 없습니다");
            }
            String trimmed = propertyKey.trim();
            if (trimmed.isBlank()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "property_key는 비어 있을 수 없습니다");
            }
            if (!seen.add(trimmed)) {
                throw new AppException(ErrorCode.BAD_REQUEST, "properties에 중복된 property_key가 포함될 수 없습니다: " + trimmed);
            }
            normalized.add(new ReorderPropertyCommandItem(trimmed, property.system()));
        }
        return normalized;
    }

    private UUID parsePropertyDefinitionId(String propertyKey) {
        try {
            return UUID.fromString(propertyKey);
        } catch (IllegalArgumentException ex) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "재정렬할 속성 key가 올바르지 않습니다: " + propertyKey
            );
        }
    }

    private int resolveCurrentDisplayOrder(
            String propertyKey,
            Map<String, SystemPropertySpec> systemSpecsByKey,
            Map<UUID, PropertyDefinition> customDefinitionsById,
            Map<String, SystemPropertyOverride> overridesByKey
    ) {
        SystemPropertySpec spec = systemSpecsByKey.get(propertyKey);
        if (spec != null) {
            SystemPropertyOverride override = overridesByKey.get(propertyKey);
            if (override != null && override.getDisplayOrder() != null) {
                return override.getDisplayOrder();
            }
            return spec.displayOrder();
        }

        PropertyDefinition definition = customDefinitionsById.get(parsePropertyDefinitionId(propertyKey));
        return definition.getDisplayOrder();
    }

    private void applySystemPropertyDisplayOrder(
            PropertyOwnerType ownerType,
            String propertyKey,
            SystemPropertySpec spec,
            Map<String, SystemPropertyOverride> overridesByKey,
            int nextDisplayOrder
    ) {
        SystemPropertyOverride override = overridesByKey.get(propertyKey);
        int currentDisplayOrder = override != null && override.getDisplayOrder() != null
                ? override.getDisplayOrder()
                : spec.displayOrder();

        if (currentDisplayOrder == nextDisplayOrder) {
            return;
        }

        if (override == null) {
            SystemPropertyOverride created = systemPropertyOverrideRepository.save(
                    SystemPropertyOverride.create(ownerType, propertyKey, null, nextDisplayOrder, true)
            );
            overridesByKey.put(propertyKey, created);
            return;
        }

        override.changeDisplayOrder(nextDisplayOrder);
    }

    private List<PropertyOptionItem> toOptionItems(List<PropertyOptionCommandItem> options) {
        if (options == null) {
            return null;
        }
        return options.stream()
                .map(option -> new PropertyOptionItem(
                        option.value(),
                        option.label(),
                        option.displayOrder(),
                        option.active()
                ))
                .toList();
    }
}
