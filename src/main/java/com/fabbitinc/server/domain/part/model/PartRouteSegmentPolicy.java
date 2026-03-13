package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.Set;
import java.util.regex.Pattern;

public final class PartRouteSegmentPolicy {

    private static final Pattern SAFE_SEGMENT_PATTERN = Pattern.compile("^[A-Za-z0-9._~-]+$");
    private static final Set<String> RESERVED_SEGMENTS = Set.of(".", "..");

    private PartRouteSegmentPolicy() {
    }

    public static String validatePartNumber(String value, String domainCode) {
        return validate(value, domainCode, "품번");
    }

    public static String validateRevisionCode(String value, String domainCode) {
        return validate(value, domainCode, "리비전 코드");
    }

    private static String validate(String value, String domainCode, String label) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (RESERVED_SEGMENTS.contains(value)) {
            throw new DomainException(domainCode, label + "는 '.' 또는 '..'일 수 없습니다");
        }
        if (!SAFE_SEGMENT_PATTERN.matcher(value).matches()) {
            throw new DomainException(
                    domainCode,
                    label + "는 영문 대소문자, 숫자, 점(.), 밑줄(_), 물결(~), 하이픈(-)만 사용할 수 있습니다"
            );
        }
        return value;
    }
}
