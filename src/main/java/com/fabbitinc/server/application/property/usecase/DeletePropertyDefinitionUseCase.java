package com.fabbitinc.server.application.property.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.service.PropertyService;
import com.fabbitinc.server.application.property.usecase.command.DeletePropertyDefinitionCommand;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeletePropertyDefinitionUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PropertyService propertyService;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(DeletePropertyDefinitionCommand command) {
        currentAuthProvider.getCurrentAuth();
        propertyService.deleteProperty(resolveOwnerType(command.ownerType()), command.propertyKey());
    }

    private PropertyOwnerType resolveOwnerType(String rawOwnerType) {
        try {
            return PropertyOwnerType.valueOf(rawOwnerType);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 owner_type입니다: " + rawOwnerType);
        }
    }
}
