package com.fabbitinc.server.domain.property.model;

import com.fabbitinc.server.domain.common.exception.DomainException;

public record PropertyOptionItem(
        String value,
        String label,
        Integer displayOrder,
        Boolean active
) {

    public static final String CODE_PROPERTY_OPTION_VALUE_REQUIRED =
            "PROPERTY_OPTION_VALUE_REQUIRED";
    public static final String CODE_PROPERTY_OPTION_VALUE_TOO_LONG =
            "PROPERTY_OPTION_VALUE_TOO_LONG";
    public static final String CODE_PROPERTY_OPTION_LABEL_REQUIRED =
            "PROPERTY_OPTION_LABEL_REQUIRED";
    public static final String CODE_PROPERTY_OPTION_LABEL_TOO_LONG =
            "PROPERTY_OPTION_LABEL_TOO_LONG";
    public static final String CODE_PROPERTY_OPTION_DISPLAY_ORDER_INVALID =
            "PROPERTY_OPTION_DISPLAY_ORDER_INVALID";

    private static final int MAX_VALUE_LENGTH = 100;
    private static final int MAX_LABEL_LENGTH = 200;

    public PropertyOptionItem {
        value = requireValue(value);
        label = requireLabel(label);
        displayOrder = normalizeDisplayOrder(displayOrder);
        active = normalizeActive(active);
    }

    private static String requireValue(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_PROPERTY_OPTION_VALUE_REQUIRED, "옵션 value는 필수입니다");
        }

        String trimmed = value.trim();
        if (trimmed.length() > MAX_VALUE_LENGTH) {
            throw new DomainException(
                    CODE_PROPERTY_OPTION_VALUE_TOO_LONG,
                    "옵션 value는 100자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private static String requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new DomainException(CODE_PROPERTY_OPTION_LABEL_REQUIRED, "옵션 label은 필수입니다");
        }

        String trimmed = label.trim();
        if (trimmed.length() > MAX_LABEL_LENGTH) {
            throw new DomainException(
                    CODE_PROPERTY_OPTION_LABEL_TOO_LONG,
                    "옵션 label은 200자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private static Integer normalizeDisplayOrder(Integer displayOrder) {
        if (displayOrder == null) {
            return 0;
        }
        if (displayOrder < 0) {
            throw new DomainException(
                    CODE_PROPERTY_OPTION_DISPLAY_ORDER_INVALID,
                    "옵션 display_order는 0 이상이어야 합니다"
            );
        }
        return displayOrder;
    }

    private static Boolean normalizeActive(Boolean active) {
        if (active == null) {
            return true;
        }
        return active;
    }
}
