package com.fabbitinc.server.application.property.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.application.property.api.PropertyDefinitionUsageSummary;
import com.fabbitinc.server.application.property.usecase.command.PropertyOptionCommandItem;
import com.fabbitinc.server.application.property.usecase.command.ReorderPropertyCommandItem;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Mock
    private PropertyApi propertyApi;

    @Test
    void createCustomProperty_중복된_표시명이면_conflict를_던진다() {
        when(propertyDefinitionRepository.existsByOwnerTypeAndDisplayName(PropertyOwnerType.PART, "표면처리"))
                .thenReturn(true);

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        AppException ex = assertThrows(
                AppException.class,
                () -> service.createCustomProperty(
                        PropertyOwnerType.PART,
                        "표면처리",
                        null,
                        PropertyValueType.STRING,
                        null,
                        List.of(),
                        10,
                        false
                )
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void updateProperty_커스텀속성은_값타입과_옵션을_재구성한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.STRING,
                null,
                List.of(),
                10,
                false
        );
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKey(
                PropertyOwnerType.PART,
                definition.getPropertyKey()
        )).thenReturn(Optional.of(definition));

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        PropertyDefinition updated = service.updateProperty(
                PropertyOwnerType.PART,
                definition.getPropertyKey(),
                "표면처리",
                false,
                null,
                false,
                PropertyValueType.OPTION,
                true,
                PropertyOptionMode.FIXED,
                true,
                List.of(new PropertyOptionCommandItem("도장", "도장", 0, true)),
                true,
                null,
                false,
                null,
                false,
                null,
                false
        );

        assertEquals(PropertyValueType.OPTION, updated.getValueType());
        assertEquals(PropertyOptionMode.FIXED, updated.getOptionMode());
        assertEquals("도장", updated.getOptions().getFirst().value());
    }

    @Test
    void updateProperty_시스템속성은_구조변경을_막는다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                PartSystemPropertyKind.MATERIAL,
                "재질",
                "부품 재질",
                PropertyValueType.STRING,
                null,
                List.of(),
                "material",
                4,
                false,
                true
        );
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKey(PropertyOwnerType.PART, "material"))
                .thenReturn(Optional.of(definition));

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        AppException ex = assertThrows(
                AppException.class,
                () -> service.updateProperty(
                        PropertyOwnerType.PART,
                        "material",
                        null,
                        false,
                        null,
                        false,
                        PropertyValueType.OPTION,
                        true,
                        null,
                        false,
                        null,
                        false,
                        null,
                        false,
                        null,
                        false,
                        null,
                        false
                )
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void deleteProperty_커스텀속성이_사용중이면_conflict를_던진다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.STRING,
                null,
                List.of(),
                10,
                false
        );
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKey(
                PropertyOwnerType.PART,
                definition.getPropertyKey()
        )).thenReturn(Optional.of(definition));
        when(propertyApi.getPropertyDefinitionUsage(definition.getPropertyKey()))
                .thenReturn(new PropertyDefinitionUsageSummary(3, 0, 0, 0));

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        AppException ex = assertThrows(
                AppException.class,
                () -> service.deleteProperty(PropertyOwnerType.PART, definition.getPropertyKey())
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void deleteProperty_시스템속성은_badRequest를_던진다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "part_number",
                PartSystemPropertyKind.PART_NUMBER,
                "품번",
                "부품의 고유 식별자",
                PropertyValueType.STRING,
                null,
                List.of(),
                "part_number",
                1,
                true,
                false
        );
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKey(PropertyOwnerType.PART, "part_number"))
                .thenReturn(Optional.of(definition));

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        AppException ex = assertThrows(
                AppException.class,
                () -> service.deleteProperty(PropertyOwnerType.PART, "part_number")
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void deleteProperty_미사용_커스텀속성은_삭제한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.STRING,
                null,
                List.of(),
                10,
                false
        );
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKey(
                PropertyOwnerType.PART,
                definition.getPropertyKey()
        )).thenReturn(Optional.of(definition));
        when(propertyApi.getPropertyDefinitionUsage(definition.getPropertyKey()))
                .thenReturn(new PropertyDefinitionUsageSummary(0, 0, 0, 0));
        doNothing().when(propertyDefinitionRepository).delete(definition);

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        service.deleteProperty(PropertyOwnerType.PART, definition.getPropertyKey());

        verify(propertyDefinitionRepository).delete(definition);
    }

    @Test
    void reorderProperties_시스템과_커스텀을_같은_catalog에서_재정렬한다() {
        PropertyDefinition material = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                PartSystemPropertyKind.MATERIAL,
                "재질",
                "부품 재질",
                PropertyValueType.STRING,
                null,
                List.of(),
                "material",
                4,
                false,
                true
        );
        PropertyDefinition custom1 = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "속성1",
                null,
                PropertyValueType.STRING,
                null,
                List.of(),
                20,
                false
        );
        PropertyDefinition custom2 = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "속성2",
                null,
                PropertyValueType.STRING,
                null,
                List.of(),
                30,
                false
        );
        when(propertyDefinitionRepository.findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(PropertyOwnerType.PART))
                .thenReturn(List.of(material, custom1, custom2));

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        service.reorderProperties(
                PropertyOwnerType.PART,
                List.of(
                        new ReorderPropertyCommandItem(custom1.getPropertyKey(), false),
                        new ReorderPropertyCommandItem("material", true),
                        new ReorderPropertyCommandItem(custom2.getPropertyKey(), false)
                )
        );

        assertEquals(1, custom1.getDisplayOrder());
        assertEquals(2, material.getDisplayOrder());
        assertEquals(3, custom2.getDisplayOrder());
    }

    @Test
    void reorderProperties_기존_displayOrder가_중복이어도_최종순서를_유일한값으로_재부여한다() {
        PropertyDefinition revision = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "revision",
                PartSystemPropertyKind.REVISION,
                "리비전",
                "부품 리비전",
                PropertyValueType.STRING,
                null,
                List.of(),
                "revision_code",
                1,
                false,
                false
        );
        PropertyDefinition custom = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "커스텀",
                null,
                PropertyValueType.STRING,
                null,
                List.of(),
                1,
                false
        );
        PropertyDefinition material = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                PartSystemPropertyKind.MATERIAL,
                "재질",
                "부품 재질",
                PropertyValueType.STRING,
                null,
                List.of(),
                "material",
                1,
                false,
                true
        );
        when(propertyDefinitionRepository.findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(PropertyOwnerType.PART))
                .thenReturn(List.of(custom, material, revision));

        PropertyService service = new PropertyService(propertyDefinitionRepository, propertyApi);

        service.reorderProperties(
                PropertyOwnerType.PART,
                List.of(
                        new ReorderPropertyCommandItem("revision", true),
                        new ReorderPropertyCommandItem(custom.getPropertyKey(), false),
                        new ReorderPropertyCommandItem("material", true)
                )
        );

        assertEquals(1, revision.getDisplayOrder());
        assertEquals(2, custom.getDisplayOrder());
        assertEquals(3, material.getDisplayOrder());
    }
}
