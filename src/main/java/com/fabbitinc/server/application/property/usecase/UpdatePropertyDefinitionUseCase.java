package com.fabbitinc.server.application.property.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.service.PropertyService;
import com.fabbitinc.server.application.property.usecase.command.UpdatePropertyDefinitionCommand;
import com.fabbitinc.server.application.property.usecase.result.UpdatePropertyDefinitionResult;
import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
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
        PropertyDefinition definition = propertyService.updateProperty(
                resolveOwnerType(command.ownerType()),
                command.propertyKey(),
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
        return new UpdatePropertyDefinitionResult(definition.getOwnerType().name(), definition.getPropertyKey());
    }

    private PropertyOwnerType resolveOwnerType(String rawOwnerType) {
        try {
            return PropertyOwnerType.valueOf(rawOwnerType);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 owner_type입니다: " + rawOwnerType);
        }
    }
}
