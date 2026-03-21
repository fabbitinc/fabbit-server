package com.fabbitinc.server.application.property.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    void validateExtendedProperties_활성_정의와_타입에_맞으면_통과한다() {
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
        Map<String, Object> properties = Map.of(definition.getId().toString(), "AL6061");
        when(propertyDefinitionRepository.findByIdInAndOwnerTypeAndActiveTrue(
                List.of(definition.getId()),
                PropertyOwnerType.PART
        )).thenReturn(List.of(definition));

        PropertyApi propertyApi = new PropertyApi(
                propertyDefinitionRepository,
                partRevisionRepository,
                supplierRepository,
                engineeringBomItemRepository,
                partSupplierRepository
        );

        Map<String, Object> validated = propertyApi.validateExtendedProperties(PropertyOwnerType.PART, properties);

        assertEquals("AL6061", validated.get(definition.getId().toString()));
    }

    @Test
    void validateExtendedProperties_key가_uuid가_아니면_예외를_던진다() {
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
    void validateExtendedProperties_fixedOption에_없는_값이면_예외를_던진다() {
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
        UUID definitionId = definition.getId();
        when(propertyDefinitionRepository.findByIdInAndOwnerTypeAndActiveTrue(
                List.of(definitionId),
                PropertyOwnerType.PART
        )).thenReturn(List.of(definition));

        PropertyApi propertyApi = new PropertyApi(
                propertyDefinitionRepository,
                partRevisionRepository,
                supplierRepository,
                engineeringBomItemRepository,
                partSupplierRepository
        );

        AppException ex = assertThrows(
                AppException.class,
                () -> propertyApi.validateExtendedProperties(
                        PropertyOwnerType.PART,
                        Map.of(definitionId.toString(), "UNKNOWN")
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void getPropertyDefinitionUsage_사용처별_건수를_집계한다() {
        UUID definitionId = UUID.randomUUID();
        when(partRevisionRepository.countByExtendedPropertiesContainingPropertyDefinitionId(definitionId.toString()))
                .thenReturn(2L);
        when(supplierRepository.countByExtendedPropertiesContainingPropertyDefinitionId(definitionId.toString()))
                .thenReturn(1L);
        when(engineeringBomItemRepository.countByExtendedPropertiesContainingPropertyDefinitionId(definitionId.toString()))
                .thenReturn(4L);
        when(partSupplierRepository.countByExtendedPropertiesContainingPropertyDefinitionId(definitionId.toString()))
                .thenReturn(3L);

        PropertyApi propertyApi = new PropertyApi(
                propertyDefinitionRepository,
                partRevisionRepository,
                supplierRepository,
                engineeringBomItemRepository,
                partSupplierRepository
        );

        PropertyDefinitionUsageSummary usage = propertyApi.getPropertyDefinitionUsage(definitionId);

        assertEquals(10L, usage.totalCount());
        assertEquals(true, usage.inUse());
    }
}
