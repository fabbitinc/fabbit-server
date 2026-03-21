package com.fabbitinc.server.application.property.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.property.service.PropertyService;
import com.fabbitinc.server.application.property.usecase.command.DeletePropertyDefinitionCommand;
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
        propertyService.deleteCustomProperty(command.propertyDefinitionId());
    }
}
