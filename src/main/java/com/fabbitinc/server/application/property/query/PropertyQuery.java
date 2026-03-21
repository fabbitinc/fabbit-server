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
import com.fabbitinc.server.domain.property.model.PropertyStorageKind;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import java.util.Comparator;
import java.util.List;
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
        List<PropertyDefinition> definitions = includeInactive
                ? propertyDefinitionRepository.findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(ownerType)
                : propertyDefinitionRepository.findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(ownerType);

        return definitions.stream()
                .map(this::toMeta)
                .sorted(Comparator
                        .comparingInt(PropertyMetaResult::displayOrder)
                        .thenComparing(PropertyMetaResult::displayName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(PropertyMetaResult::propertyKey))
                .toList();
    }

    private PropertyMetaResult toMeta(PropertyDefinition definition) {
        return new PropertyMetaResult(
                definition.getId(),
                definition.getOwnerType(),
                definition.getPropertyKey(),
                definition.isSystemProperty(),
                definition.getPartSystemPropertyKind(),
                definition.isActiveConfigurable(),
                definition.getStorageKind() == PropertyStorageKind.COLUMN ? definition.getStorageBinding() : null,
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
