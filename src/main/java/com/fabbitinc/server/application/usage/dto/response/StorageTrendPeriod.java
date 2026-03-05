package com.fabbitinc.server.application.usage.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;

public enum StorageTrendPeriod {
    DAYS_7("7d"),
    DAYS_30("30d"),
    YEAR_1("1y");

    private final String value;

    StorageTrendPeriod(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static StorageTrendPeriod from(String rawValue) {
        for (StorageTrendPeriod candidate : values()) {
            if (candidate.value.equals(rawValue)) {
                return candidate;
            }
        }
        throw new AppException(
                ErrorCode.VALIDATION_ERROR,
                "period는 '7d', '30d', '1y' 중 하나여야 합니다"
        );
    }
}
