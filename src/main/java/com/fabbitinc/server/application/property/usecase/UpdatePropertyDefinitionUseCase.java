package com.fabbitinc.server.application.property.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.property.service.PropertyService;
import com.fabbitinc.server.application.property.usecase.command.UpdatePropertyDefinitionCommand;
import com.fabbitinc.server.application.property.usecase.result.UpdatePropertyDefinitionResult;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdatePropertyDefinitionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PropertyService propertyService;

    @PreAuthorize("hasRole('ADMIN')")
    public UpdatePropertyDefinitionResult execute(UpdatePropertyDefinitionCommand command) {
        currentAuthProvider.getCurrentAuth();
        PropertyDefinition definition = propertyService.updateCustomProperty(
                command.propertyDefinitionId(),
                command.displayName(),
                command.displayNameSet(),
                command.description(),
                command.descriptionSet(),
                command.valueType(),
                command.valueTypeSet(),
                command.optionMode(),
                command.optionModeSet(),
                command.options(),
                command.optionsSet(),
                command.displayOrder(),
                command.displayOrderSet(),
                command.required(),
                command.requiredSet(),
                command.active(),
                command.activeSet()
        );
        return new UpdatePropertyDefinitionResult(definition.getId(), definition.getOwnerType().name());
    }
}
