package com.fabbitinc.server.application.property.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyApi {

    private final PropertyDefinitionRepository propertyDefinitionRepository;

    public Map<String, Object> validateExtendedProperties(
            PropertyOwnerType ownerType,
            Map<String, Object> extendedProperties
    ) {
        if (extendedProperties == null || extendedProperties.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        List<UUID> definitionIds = parseDefinitionIds(extendedProperties.keySet());
        Map<UUID, PropertyDefinition> definitionsById = propertyDefinitionRepository
                .findByIdInAndOwnerTypeAndActiveTrue(definitionIds, ownerType)
                .stream()
                .collect(Collectors.toMap(PropertyDefinition::getId, definition -> definition));

        Set<UUID> missingIds = definitionIds.stream()
                .filter(id -> !definitionsById.containsKey(id))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!missingIds.isEmpty()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "유효하지 않은 확장 속성 정의 ID가 있습니다: " + missingIds
            );
        }

        for (Map.Entry<String, Object> entry : extendedProperties.entrySet()) {
            UUID definitionId = UUID.fromString(entry.getKey());
            PropertyDefinition definition = definitionsById.get(definitionId);
            normalized.put(entry.getKey(), normalizeValue(definition, entry.getValue()));
        }
        return normalized;
    }

    private List<UUID> parseDefinitionIds(Collection<String> propertyKeys) {
        return propertyKeys.stream()
                .map(this::parseDefinitionId)
                .toList();
    }

    private UUID parseDefinitionId(String propertyKey) {
        try {
            return UUID.fromString(propertyKey);
        } catch (IllegalArgumentException ex) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "확장 속성 key는 property_definition.id(UUID)여야 합니다: " + propertyKey
            );
        }
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
