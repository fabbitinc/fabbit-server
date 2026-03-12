package com.fabbitinc.server.domain.property.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

class PropertyOptionItemTest {

    @Test
    void 생성시_입력값을_trim하고_기본값을_채운다() {
        PropertyOptionItem item = new PropertyOptionItem("  plating  ", "  도금  ", null, null);

        assertEquals("plating", item.value());
        assertEquals("도금", item.label());
        assertEquals(0, item.displayOrder());
        assertTrue(item.active());
    }

    @Test
    void value가_없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> new PropertyOptionItem(" ", "도금", 0, true));

        assertEquals(PropertyOptionItem.CODE_PROPERTY_OPTION_VALUE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void label이_없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> new PropertyOptionItem("plating", " ", 0, true));

        assertEquals(PropertyOptionItem.CODE_PROPERTY_OPTION_LABEL_REQUIRED, ex.getDomainCode());
    }

    @Test
    void displayOrder가_음수면_예외를_던진다() {
        DomainException ex = assertThrows(
                DomainException.class,
                () -> new PropertyOptionItem("plating", "도금", -1, true)
        );

        assertEquals(PropertyOptionItem.CODE_PROPERTY_OPTION_DISPLAY_ORDER_INVALID, ex.getDomainCode());
    }
}
