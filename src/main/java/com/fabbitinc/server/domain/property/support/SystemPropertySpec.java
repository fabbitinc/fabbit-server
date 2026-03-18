package com.fabbitinc.server.domain.property.support;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.property.model.PropertyOptionItem;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record SystemPropertySpec(
        PropertyOwnerType ownerType,
        String propertyKey,
        PartSystemPropertyKind partSystemPropertyKind,
        String displayName,
        String description,
        PropertyValueType valueType,
        PropertyOptionMode optionMode,
        List<PropertyOptionItem> options,
        String columnName,
        int displayOrder,
        boolean required
) {

    public static final String CODE_SYSTEM_PROPERTY_SPEC_OWNER_TYPE_REQUIRED =
            "SYSTEM_PROPERTY_SPEC_OWNER_TYPE_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_PROPERTY_KEY_REQUIRED =
            "SYSTEM_PROPERTY_SPEC_PROPERTY_KEY_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_DISPLAY_NAME_REQUIRED =
            "SYSTEM_PROPERTY_SPEC_DISPLAY_NAME_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_VALUE_TYPE_REQUIRED =
            "SYSTEM_PROPERTY_SPEC_VALUE_TYPE_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_COLUMN_NAME_REQUIRED =
            "SYSTEM_PROPERTY_SPEC_COLUMN_NAME_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_OPTION_MODE_NOT_ALLOWED =
            "SYSTEM_PROPERTY_SPEC_OPTION_MODE_NOT_ALLOWED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_OPTIONS_NOT_ALLOWED =
            "SYSTEM_PROPERTY_SPEC_OPTIONS_NOT_ALLOWED";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_DUPLICATE_OPTION_VALUE =
            "SYSTEM_PROPERTY_SPEC_DUPLICATE_OPTION_VALUE";
    public static final String CODE_SYSTEM_PROPERTY_SPEC_DISPLAY_ORDER_INVALID =
            "SYSTEM_PROPERTY_SPEC_DISPLAY_ORDER_INVALID";

    public SystemPropertySpec {
        ownerType = requireOwnerType(ownerType);
        propertyKey = requireText(
                propertyKey,
                CODE_SYSTEM_PROPERTY_SPEC_PROPERTY_KEY_REQUIRED,
                "시스템 속성 property_key는 필수입니다"
        );
        displayName = requireText(
                displayName,
                CODE_SYSTEM_PROPERTY_SPEC_DISPLAY_NAME_REQUIRED,
                "시스템 속성 표시명은 필수입니다"
        );
        valueType = requireValueType(valueType);
        optionMode = normalizeOptionMode(optionMode, valueType);
        options = normalizeOptions(options, valueType);
        columnName = requireText(
                columnName,
                CODE_SYSTEM_PROPERTY_SPEC_COLUMN_NAME_REQUIRED,
                "시스템 속성 column_name은 필수입니다"
        );
        partSystemPropertyKind = normalizePartSystemPropertyKind(ownerType, partSystemPropertyKind);
        displayOrder = requireDisplayOrder(displayOrder);
    }

    private static PropertyOwnerType requireOwnerType(PropertyOwnerType ownerType) {
        if (ownerType == null) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_SPEC_OWNER_TYPE_REQUIRED,
                    "시스템 속성 소유 타입은 필수입니다"
            );
        }
        return ownerType;
    }

    private static String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(code, message);
        }
        return value.trim();
    }

    private static PropertyValueType requireValueType(PropertyValueType valueType) {
        if (valueType == null) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_SPEC_VALUE_TYPE_REQUIRED,
                    "시스템 속성 값 타입은 필수입니다"
            );
        }
        return valueType;
    }

    private static PropertyOptionMode normalizeOptionMode(
            PropertyOptionMode optionMode,
            PropertyValueType valueType
    ) {
        if (valueType != PropertyValueType.OPTION) {
            if (optionMode == null) {
                return null;
            }
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_SPEC_OPTION_MODE_NOT_ALLOWED,
                    "OPTION 타입이 아닌 시스템 속성은 option_mode를 가질 수 없습니다"
            );
        }

        if (optionMode == null) {
            return PropertyOptionMode.FIXED;
        }
        return optionMode;
    }

    private static List<PropertyOptionItem> normalizeOptions(
            List<PropertyOptionItem> options,
            PropertyValueType valueType
    ) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        if (valueType != PropertyValueType.OPTION) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_SPEC_OPTIONS_NOT_ALLOWED,
                    "OPTION 타입이 아닌 시스템 속성은 옵션 목록을 가질 수 없습니다"
            );
        }

        List<PropertyOptionItem> normalized = List.copyOf(options);
        Set<String> optionValues = new LinkedHashSet<>();
        for (PropertyOptionItem option : normalized) {
            if (!optionValues.add(option.value())) {
                throw new DomainException(
                        CODE_SYSTEM_PROPERTY_SPEC_DUPLICATE_OPTION_VALUE,
                        "시스템 속성 옵션 value는 중복될 수 없습니다"
                );
            }
        }
        return normalized;
    }

    private static int requireDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_SPEC_DISPLAY_ORDER_INVALID,
                    "시스템 속성 display_order는 0 이상이어야 합니다"
            );
        }
        return displayOrder;
    }

    private static PartSystemPropertyKind normalizePartSystemPropertyKind(
            PropertyOwnerType ownerType,
            PartSystemPropertyKind partSystemPropertyKind
    ) {
        if (ownerType == PropertyOwnerType.PART && partSystemPropertyKind == null) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_SPEC_OWNER_TYPE_REQUIRED,
                    "PART 시스템 속성은 partSystemPropertyKind가 필수입니다"
            );
        }
        if (ownerType != PropertyOwnerType.PART) {
            return null;
        }
        return partSystemPropertyKind;
    }
}
