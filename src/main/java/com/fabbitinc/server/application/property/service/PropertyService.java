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
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyDefinitionRepository propertyDefinitionRepository;
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

    public PropertyDefinition updateProperty(
            PropertyOwnerType ownerType,
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
        try {
            PropertyDefinition definition = getRequiredDefinition(ownerType, propertyKey);

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
            if (displayOrderSet) {
                requireNullableField("display_order", displayOrder);
                definition.reorder(displayOrder);
            }

            if (definition.isSystemProperty()) {
                validateSystemMutation(valueTypeSet, optionModeSet, optionsSet, requiredSet);
                if (activeSet) {
                    requireNullableField("active", active);
                    applyActive(definition, active);
                }
                return definition;
            }

            PropertyValueType nextValueType = valueTypeSet ? valueType : definition.getValueType();
            PropertyOptionMode nextOptionMode = optionModeSet ? optionMode : definition.getOptionMode();
            List<PropertyOptionItem> nextOptions = optionsSet ? toOptionItems(options) : definition.getOptions();
            if (valueTypeSet || optionModeSet || optionsSet) {
                definition.reconfigureValueSpec(nextValueType, nextOptionMode, nextOptions);
            }

            if (requiredSet) {
                requireNullableField("required", required);
                if (required) {
                    definition.markRequired();
                } else {
                    definition.markOptional();
                }
            }
            if (activeSet) {
                requireNullableField("active", active);
                applyActive(definition, active);
            }
            return definition;
        } catch (DomainException ex) {
            throw toValidationException(ex);
        }
    }

    public void deleteProperty(PropertyOwnerType ownerType, String propertyKey) {
        PropertyDefinition definition = getRequiredDefinition(ownerType, propertyKey);
        if (definition.isSystemProperty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "시스템 속성은 삭제할 수 없습니다: " + propertyKey);
        }

        PropertyDefinitionUsageSummary usageSummary = propertyApi.getPropertyDefinitionUsage(definition.getPropertyKey());
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
        List<PropertyDefinition> allDefinitions = propertyDefinitionRepository
                .findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(ownerType);
        Map<String, PropertyDefinition> definitionsByKey = allDefinitions
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PropertyDefinition::getPropertyKey,
                        definition -> definition
                ));

        if (definitionsByKey.size() != normalizedProperties.size()) {
            Set<String> missingKeys = normalizedProperties.stream()
                    .map(ReorderPropertyCommandItem::propertyKey)
                    .filter(propertyKey -> !definitionsByKey.containsKey(propertyKey))
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "재정렬할 속성을 찾을 수 없습니다: " + missingKeys
            );
        }

        for (ReorderPropertyCommandItem property : normalizedProperties) {
            PropertyDefinition definition = definitionsByKey.get(property.propertyKey());
            if (definition.isSystemProperty() != property.system()) {
                throw new AppException(
                        ErrorCode.BAD_REQUEST,
                        "system 플래그가 실제 속성 타입과 다릅니다: " + property.propertyKey()
                );
            }
        }

        java.util.List<PropertyDefinition> reorderedDefinitions = new java.util.ArrayList<>(allDefinitions.size());
        Set<String> submittedKeys = normalizedProperties.stream()
                .map(ReorderPropertyCommandItem::propertyKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        for (ReorderPropertyCommandItem property : normalizedProperties) {
            reorderedDefinitions.add(definitionsByKey.get(property.propertyKey()));
        }
        allDefinitions.stream()
                .filter(definition -> !submittedKeys.contains(definition.getPropertyKey()))
                .forEach(reorderedDefinitions::add);

        for (int index = 0; index < reorderedDefinitions.size(); index++) {
            PropertyDefinition definition = reorderedDefinitions.get(index);
            int nextDisplayOrder = index + 1;
            if (definition.getDisplayOrder() != nextDisplayOrder) {
                definition.reorder(nextDisplayOrder);
            }
        }
    }

    private void validateSystemMutation(
            boolean valueTypeSet,
            boolean optionModeSet,
            boolean optionsSet,
            boolean requiredSet
    ) {
        if (valueTypeSet || optionModeSet || optionsSet || requiredSet) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "시스템 속성은 표시명/설명/표시 순서/활성 여부만 수정할 수 있습니다"
            );
        }
    }

    private void applyActive(PropertyDefinition definition, Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            definition.activate();
            return;
        }
        definition.deactivate();
    }

    private void requireNullableField(String fieldName, Object value) {
        if (value == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, fieldName + "는 null일 수 없습니다");
        }
    }

    private PropertyDefinition getRequiredDefinition(PropertyOwnerType ownerType, String propertyKey) {
        return propertyDefinitionRepository.findByOwnerTypeAndPropertyKey(ownerType, propertyKey)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "속성 정의 '%s/%s'을(를) 찾을 수 없습니다".formatted(ownerType, propertyKey)
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
