package com.fabbitinc.server.application.property.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.usecase.command.PropertyOptionCommandItem;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.model.SystemPropertyOverride;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.repository.SystemPropertyOverrideRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Mock
    private SystemPropertyOverrideRepository systemPropertyOverrideRepository;

    @Test
    void createCustomProperty_중복된_표시명이면_conflict를_던진다() {
        when(propertyDefinitionRepository.existsByOwnerTypeAndDisplayName(PropertyOwnerType.PART, "표면처리"))
                .thenReturn(true);

        PropertyService service = new PropertyService(propertyDefinitionRepository, systemPropertyOverrideRepository);

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
    void updateCustomProperty_값타입과_옵션을_함께_재구성한다() {
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
        when(propertyDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));
        PropertyService service = new PropertyService(propertyDefinitionRepository, systemPropertyOverrideRepository);

        PropertyDefinition updated = service.updateCustomProperty(
                definition.getId(),
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
    void upsertSystemPropertyOverride_없는_override면_생성해서_저장한다() {
        when(systemPropertyOverrideRepository.findByOwnerTypeAndPropertyKey(PropertyOwnerType.PART, "category"))
                .thenReturn(Optional.empty());
        when(systemPropertyOverrideRepository.save(any(SystemPropertyOverride.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PropertyService service = new PropertyService(propertyDefinitionRepository, systemPropertyOverrideRepository);

        SystemPropertyOverride override = service.upsertSystemPropertyOverride(
                PropertyOwnerType.PART,
                "category",
                "품목군",
                99,
                false
        );

        assertEquals("품목군", override.getDisplayNameOverride());
        assertEquals(99, override.getDisplayOrder());
        assertEquals(false, override.isActive());
        verify(systemPropertyOverrideRepository).save(override);
    }
}
