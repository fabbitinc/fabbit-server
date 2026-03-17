package com.fabbitinc.server.application.property.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.usecase.command.PropertyOptionCommandItem;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final SystemPropertyOverrideRepository systemPropertyOverrideRepository;

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

    public SystemPropertyOverride upsertSystemPropertyOverride(
            PropertyOwnerType ownerType,
            String propertyKey,
            String displayNameOverride,
            Integer displayOrder,
            Boolean active
    ) {
        try {
            if (SystemPropertyRegistry.find(ownerType, propertyKey).isEmpty()) {
                throw new AppException(
                        ErrorCode.BAD_REQUEST,
                        "시스템 속성 '%s/%s'은(는) 존재하지 않습니다".formatted(ownerType, propertyKey)
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
