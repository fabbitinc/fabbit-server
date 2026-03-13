package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

class PartRouteSegmentPolicyTest {

    @Test
    void validatePartNumber_url_segment로_안전한_문자는_허용한다() {
        String normalized = PartRouteSegmentPolicy.validatePartNumber("AES-100_A.1~sample", "PART_NUMBER_INVALID");

        assertEquals("AES-100_A.1~sample", normalized);
    }

    @Test
    void validatePartNumber_슬래시가_포함되면_예외를_던진다() {
        DomainException ex = assertThrows(
                DomainException.class,
                () -> PartRouteSegmentPolicy.validatePartNumber("AES/100", "PART_NUMBER_INVALID")
        );

        assertEquals("PART_NUMBER_INVALID", ex.getDomainCode());
    }

    @Test
    void validateRevisionCode_dot_segment는_예외를_던진다() {
        DomainException ex = assertThrows(
                DomainException.class,
                () -> PartRouteSegmentPolicy.validateRevisionCode(".", "REVISION_CODE_INVALID")
        );

        assertEquals("REVISION_CODE_INVALID", ex.getDomainCode());
    }

    @Test
    void validateRevisionCode_공백이_포함되면_예외를_던진다() {
        DomainException ex = assertThrows(
                DomainException.class,
                () -> PartRouteSegmentPolicy.validateRevisionCode("A 1", "REVISION_CODE_INVALID")
        );

        assertEquals("REVISION_CODE_INVALID", ex.getDomainCode());
    }
}
