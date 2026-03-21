package com.fabbitinc.server.application.property.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionItem;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyStorageKind;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyApiTest {

    @Mock
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Mock
    private PartRevisionRepository partRevisionRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private EngineeringBomItemRepository engineeringBomItemRepository;

    @Mock
    private PartSupplierRepository partSupplierRepository;

    @Test
    void validateExtendedProperties_활성_catalog_key와_타입에_맞으면_통과한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "표면처리",
                null,
                PropertyValueType.OPTION,
                PropertyOptionMode.FIXED,
                List.of(new PropertyOptionItem("AL6061", "AL6061", 0, true)),
                10,
                false
        );
        Map<String, Object> properties = Map.of(definition.getPropertyKey(), "AL6061");
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKeyInAndActiveTrueAndStorageKind(
                PropertyOwnerType.PART,
                properties.keySet(),
                PropertyStorageKind.EXTENDED_PROPERTY
        )).thenReturn(List.of(definition));

        PropertyApi propertyApi = new PropertyApi(
                propertyDefinitionRepository,
                partRevisionRepository,
                supplierRepository,
                engineeringBomItemRepository,
                partSupplierRepository
        );

        Map<String, Object> validated = propertyApi.validateExtendedProperties(PropertyOwnerType.PART, properties);

        assertEquals("AL6061", validated.get(definition.getPropertyKey()));
    }

    @Test
    void validateExtendedProperties_없는_key면_예외를_던진다() {
        when(propertyDefinitionRepository.findByOwnerTypeAndPropertyKeyInAndActiveTrueAndStorageKind(
                eq(PropertyOwnerType.PART),
                anyCollection(),
                eq(PropertyStorageKind.EXTENDED_PROPERTY)
        )).thenReturn(List.of());

        PropertyApi propertyApi = new PropertyApi(
                propertyDefinitionRepository,
                partRevisionRepository,
                supplierRepository,
                engineeringBomItemRepository,
                partSupplierRepository
        );

        AppException ex = assertThrows(
                AppException.class,
                () -> propertyApi.validateExtendedProperties(PropertyOwnerType.PART, Map.of("weight", 1))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void getPropertyDefinitionUsage_사용처별_건수를_집계한다() {
        String propertyKey = "019d0000-0000-7000-8000-000000000001";
        when(partRevisionRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey))
                .thenReturn(2L);
        when(supplierRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey))
                .thenReturn(1L);
        when(engineeringBomItemRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey))
                .thenReturn(4L);
        when(partSupplierRepository.countByExtendedPropertiesContainingPropertyDefinitionId(propertyKey))
                .thenReturn(3L);

        PropertyApi propertyApi = new PropertyApi(
                propertyDefinitionRepository,
                partRevisionRepository,
                supplierRepository,
                engineeringBomItemRepository,
                partSupplierRepository
        );

        PropertyDefinitionUsageSummary usage = propertyApi.getPropertyDefinitionUsage(propertyKey);

        assertEquals(10L, usage.totalCount());
        assertEquals(true, usage.inUse());
    }
}
