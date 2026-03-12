package com.fabbitinc.server.domain.property.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyDefinitionTest {

    @Test
    void defineSystemProperty_입력값을_trim_정규화한다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "  material  ",
                "  재질  ",
                "  부품 재질  ",
                PropertyValueType.STRING,
                null,
                "  material  ",
                40,
                false
        );

        assertEquals(PropertyOwnerType.PART, definition.getOwnerType());
        assertEquals("material", definition.getPropertyKey());
        assertEquals("재질", definition.getDisplayName());
        assertEquals("부품 재질", definition.getDescription());
        assertEquals(List.of(), definition.getOptions());
        assertEquals("material", definition.getColumnName());
        assertEquals(40, definition.getDisplayOrder());
        assertEquals(PropertyValueType.STRING, definition.getValueType());
    }

    @Test
    void defineSystemProperty_column_name이_없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                "재질",
                null,
                PropertyValueType.STRING,
                null,
                " ",
                40,
                false
        ));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_COLUMN_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void defineSystemProperty_property_key가_없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                " ",
                "표면처리",
                null,
                PropertyValueType.STRING,
                null,
                "material",
                200,
                false
        ));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_PROPERTY_KEY_REQUIRED, ex.getDomainCode());
    }

    @Test
    void defineCustomProperty_커스텀_속성은_column_name이_null이다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.OPTION,
                List.of(
                        new PropertyOptionItem("plating", "도금", 10, true),
                        new PropertyOptionItem("anodizing", "아노다이징", 20, true)
                ),
                200,
                false
        );

        assertNull(definition.getPropertyKey());
        assertNull(definition.getColumnName());
        assertEquals(2, definition.getOptions().size());
        assertEquals("plating", definition.getOptions().get(0).value());
        assertEquals("도금", definition.getOptions().get(0).label());
        assertEquals(PropertyValueType.OPTION, definition.getValueType());
    }

    @Test
    void defineCustomProperty_options가_없으면_빈배열로_정규화한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.STRING,
                null,
                200,
                false
        );

        assertEquals(List.of(), definition.getOptions());
    }

    @Test
    void defineSystemProperty_option타입이_아니면_옵션목록을_가질_수없다() {
        DomainException ex = assertThrows(DomainException.class, () -> PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                "재질",
                null,
                PropertyValueType.STRING,
                List.of(new PropertyOptionItem("steel", "강재", 10, true)),
                "material",
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
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                "재질",
                null,
                PropertyValueType.STRING,
                null,
                "material",
                40,
                false
        );

        DomainException ex = assertThrows(DomainException.class, () -> definition.reorder(-1));

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_DISPLAY_ORDER_INVALID, ex.getDomainCode());
    }

    @Test
    void changeDescription_공백문자열이면_null로_정규화한다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                "재질",
                "초기 설명",
                PropertyValueType.STRING,
                null,
                "material",
                40,
                false
        );

        definition.changeDescription("   ");

        assertNull(definition.getDescription());
    }

    @Test
    void renameDisplayName과_deactivate가_가능하다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.SUPPLIER,
                "company_name",
                "공급사명",
                null,
                PropertyValueType.STRING,
                null,
                "company_name",
                10,
                true
        );

        definition.renameDisplayName("거래처명");
        definition.deactivate();

        assertEquals("거래처명", definition.getDisplayName());
        assertFalse(definition.isActive());
    }

    @Test
    void changeOptions_OPTION타입이면_옵션목록을_변경할_수있다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.OPTION,
                null,
                100,
                false
        );

        definition.changeOptions(List.of(new PropertyOptionItem("plating", "도금", 10, true)));

        assertEquals(1, definition.getOptions().size());
        assertEquals("plating", definition.getOptions().get(0).value());
    }
}
