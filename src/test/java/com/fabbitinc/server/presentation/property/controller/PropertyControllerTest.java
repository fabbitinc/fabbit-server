package com.fabbitinc.server.presentation.property.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.property.query.PropertyQuery;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaCondition;
import com.fabbitinc.server.application.property.query.result.PropertyMetaResult;
import com.fabbitinc.server.application.property.usecase.CreatePropertyDefinitionUseCase;
import com.fabbitinc.server.application.property.usecase.DeletePropertyDefinitionUseCase;
import com.fabbitinc.server.application.property.usecase.ReorderPropertyUseCase;
import com.fabbitinc.server.application.property.usecase.UpdatePropertyDefinitionUseCase;
import com.fabbitinc.server.application.property.usecase.command.DeletePropertyDefinitionCommand;
import com.fabbitinc.server.application.property.usecase.result.UpdatePropertyDefinitionResult;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.presentation.property.request.UpdatePropertyDefinitionRequest;
import com.fabbitinc.server.presentation.property.response.PropertyMetaResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PropertyControllerTest {

    @Mock
    private PropertyQuery propertyQuery;
    @Mock
    private CreatePropertyDefinitionUseCase createPropertyDefinitionUseCase;
    @Mock
    private DeletePropertyDefinitionUseCase deletePropertyDefinitionUseCase;
    @Mock
    private ReorderPropertyUseCase reorderPropertyUseCase;
    @Mock
    private UpdatePropertyDefinitionUseCase updatePropertyDefinitionUseCase;

    @InjectMocks
    private PropertyController propertyController;

    @Test
    void updatePropertyDefinition_통합_key기반으로_수정후_메타를_반환한다() {
        UpdatePropertyDefinitionRequest request = new UpdatePropertyDefinitionRequest();
        request.setDisplayName("재질명");
        request.setActive(true);
        when(updatePropertyDefinitionUseCase.execute(any()))
                .thenReturn(new UpdatePropertyDefinitionResult("PART", "material"));
        when(propertyQuery.get(any(PropertyMetaCondition.class)))
                .thenReturn(new PropertyMetaResult(
                        UUID.randomUUID(),
                        PropertyOwnerType.PART,
                        "material",
                        true,
                        com.fabbitinc.server.domain.property.support.PartSystemPropertyKind.MATERIAL,
                        true,
                        "material",
                        "재질명",
                        "부품 재질",
                        PropertyValueType.STRING,
                        null,
                        List.of(),
                        4,
                        false,
                        true
                ));

        PropertyMetaResponse response = propertyController.updatePropertyDefinition("PART", "material", request);

        assertEquals("material", response.propertyKey());
        assertEquals("재질명", response.displayName());
        assertEquals(true, response.system());
        verify(updatePropertyDefinitionUseCase).execute(any());
        verify(propertyQuery).get(new PropertyMetaCondition("PART", "material", true));
    }

    @Test
    void deletePropertyDefinition_ownerType과_propertyKey를_그대로_전달한다() {
        var response = propertyController.deletePropertyDefinition("PART", "019d0000-0000-7000-8000-000000000001");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deletePropertyDefinitionUseCase).execute(
                new DeletePropertyDefinitionCommand("PART", "019d0000-0000-7000-8000-000000000001")
        );
    }
}
