package com.fabbitinc.server.application.property.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyStorageKind;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyApi {

    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final SupplierRepository supplierRepository;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartSupplierRepository partSupplierRepository;

    public Map<String, Object> validateExtendedProperties(
            PropertyOwnerType ownerType,
            Map<String, Object> extendedProperties
    ) {
        if (extendedProperties == null || extendedProperties.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, PropertyDefinition> definitionsByKey = propertyDefinitionRepository
                .findByOwnerTypeAndPropertyKeyInAndActiveTrueAndStorageKind(
                        ownerType,
                        extendedProperties.keySet(),
                        PropertyStorageKind.EXTENDED_PROPERTY
                )
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PropertyDefinition::getPropertyKey,
                        definition -> definition
                ));

        Set<String> missingKeys = extendedProperties.keySet().stream()
                .filter(propertyKey -> !definitionsByKey.containsKey(propertyKey))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!missingKeys.isEmpty()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "유효하지 않은 확장 속성 key가 있습니다: " + missingKeys
            );
        }

        for (Map.Entry<String, Object> entry : extendedProperties.entrySet()) {
            PropertyDefinition definition = definitionsByKey.get(entry.getKey());
            normalized.put(entry.getKey(), normalizeValue(definition, entry.getValue()));
        }
        return normalized;
    }

    public PropertyDefinitionUsageSummary getPropertyDefinitionUsage(String propertyKey) {
        return new PropertyDefinitionUsageSummary(
                partRevisionRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey),
                supplierRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey),
                engineeringBomItemRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey),
                partSupplierRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey)
        );
    }

    private Object normalizeValue(PropertyDefinition definition, Object rawValue) {
        if (rawValue == null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "확장 속성 '%s' 값은 null일 수 없습니다".formatted(definition.getDisplayName())
            );
        }

        return switch (definition.getValueType()) {
            case STRING -> normalizeStringValue(definition, rawValue);
            case INTEGER -> normalizeIntegerValue(definition, rawValue);
            case FLOAT -> normalizeFloatValue(definition, rawValue);
            case BOOLEAN -> normalizeBooleanValue(definition, rawValue);
            case OPTION -> normalizeOptionValue(definition, rawValue);
        };
    }

    private String normalizeStringValue(PropertyDefinition definition, Object rawValue) {
        if (!(rawValue instanceof String value)) {
            throw invalidType(definition, "문자열");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "확장 속성 '%s' 값은 비어 있을 수 없습니다".formatted(definition.getDisplayName())
            );
        }
        return trimmed;
    }

    private Integer normalizeIntegerValue(PropertyDefinition definition, Object rawValue) {
        if (!(rawValue instanceof Number number)) {
            throw invalidType(definition, "정수");
        }
        BigDecimal decimal = new BigDecimal(number.toString());
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException ex) {
            throw invalidType(definition, "정수");
        }
    }

    private Double normalizeFloatValue(PropertyDefinition definition, Object rawValue) {
        if (!(rawValue instanceof Number number)) {
            throw invalidType(definition, "실수");
        }
        return number.doubleValue();
    }

    private Boolean normalizeBooleanValue(PropertyDefinition definition, Object rawValue) {
        if (!(rawValue instanceof Boolean value)) {
            throw invalidType(definition, "불린");
        }
        return value;
    }

    private String normalizeOptionValue(PropertyDefinition definition, Object rawValue) {
        if (!(rawValue instanceof String value)) {
            throw invalidType(definition, "옵션 문자열");
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "확장 속성 '%s' 값은 비어 있을 수 없습니다".formatted(definition.getDisplayName())
            );
        }

        if (definition.getOptionMode() == PropertyOptionMode.FIXED) {
            boolean exists = definition.getOptions().stream()
                    .anyMatch(option -> option.active() && option.value().equals(trimmed));
            if (!exists) {
                throw new AppException(
                        ErrorCode.VALIDATION_ERROR,
                        "확장 속성 '%s' 값은 미리 정의된 옵션이어야 합니다".formatted(definition.getDisplayName())
                );
            }
        }
        return trimmed;
    }

    private AppException invalidType(PropertyDefinition definition, String expectedType) {
        return new AppException(
                ErrorCode.VALIDATION_ERROR,
                "확장 속성 '%s' 값 타입이 올바르지 않습니다. 기대 타입: %s".formatted(
                        definition.getDisplayName(),
                        expectedType
                )
        );
    }
}
