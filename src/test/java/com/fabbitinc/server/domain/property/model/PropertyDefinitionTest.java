package com.fabbitinc.server.domain.property.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyDefinitionTest {

    @Test
    void defineCustomProperty_입력값을_trim_정규화한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "  표면처리  ",
                "  부품 표면 처리  ",
                PropertyValueType.STRING,
                null,
                null,
                40,
                false
        );

        assertEquals(PropertyOwnerType.PART, definition.getOwnerType());
        assertEquals("표면처리", definition.getDisplayName());
        assertEquals("부품 표면 처리", definition.getDescription());
        assertEquals(PropertyValueType.STRING, definition.getValueType());
        assertEquals(null, definition.getOptionMode());
        assertEquals(List.of(), definition.getOptions());
        assertEquals(40, definition.getDisplayOrder());
    }

    @Test
    void defineCustomProperty_OPTION타입이면_optionMode가_없어도_FIXED로_정규화한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "카테고리",
                null,
                PropertyValueType.OPTION,
                null,
                List.of(),
                100,
                false
        );

        assertEquals(PropertyOptionMode.FIXED, definition.getOptionMode());
    }

    @Test
    void defineCustomProperty_OPTION_CREATABLE을_설정할_수_있다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "카테고리",
                null,
                PropertyValueType.OPTION,
                PropertyOptionMode.CREATABLE,
                List.of(new PropertyOptionItem("machining", "가공", 10, true)),
                100,
                false
        );

        assertEquals(PropertyOptionMode.CREATABLE, definition.getOptionMode());
        assertEquals(1, definition.getOptions().size());
    }

    @Test
    void defineCustomProperty_OPTION타입이_아니면_optionMode를_가질_수없다() {
        DomainException ex = assertThrows(DomainException.class, () -> PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "설명",
                null,
                PropertyValueType.STRING,
                PropertyOptionMode.CREATABLE,
                null,
                40,
                false
        ));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_OPTION_MODE_NOT_ALLOWED, ex.getDomainCode());
    }

    @Test
    void defineCustomProperty_OPTION타입이_아니면_옵션목록을_가질_수없다() {
        DomainException ex = assertThrows(DomainException.class, () -> PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "설명",
                null,
                PropertyValueType.STRING,
                null,
                List.of(new PropertyOptionItem("foo", "Foo", 10, true)),
                40,
                false
        ));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_OPTIONS_NOT_ALLOWED, ex.getDomainCode());
    }

    @Test
    void defineCustomProperty_옵션value는_중복될_수없다() {
        DomainException ex = assertThrows(DomainException.class, () -> PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.OPTION,
                null,
                List.of(
                        new PropertyOptionItem("plating", "도금", 10, true),
                        new PropertyOptionItem("plating", "도금2", 20, true)
                ),
                200,
                false
        ));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_DUPLICATE_OPTION_VALUE, ex.getDomainCode());
    }

    @Test
    void reorder_음수면_예외를_던진다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.STRING,
                null,
                null,
                40,
                false
        );

        DomainException ex = assertThrows(DomainException.class, () -> definition.reorder(-1));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_DISPLAY_ORDER_INVALID, ex.getDomainCode());
    }

    @Test
    void renameDisplayName과_deactivate가_가능하다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.SUPPLIER,
                "비고",
                null,
                PropertyValueType.STRING,
                null,
                null,
                10,
                true
        );

        definition.renameDisplayName("메모");
        definition.deactivate();

        assertEquals("메모", definition.getDisplayName());
        assertFalse(definition.isActive());
    }
}
