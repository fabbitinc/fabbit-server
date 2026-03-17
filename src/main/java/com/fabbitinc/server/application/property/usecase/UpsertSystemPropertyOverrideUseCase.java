package com.fabbitinc.server.application.property.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.service.PropertyService;
import com.fabbitinc.server.application.property.usecase.command.UpsertSystemPropertyOverrideCommand;
import com.fabbitinc.server.application.property.usecase.result.UpsertSystemPropertyOverrideResult;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.SystemPropertyOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpsertSystemPropertyOverrideUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final PropertyService propertyService;

    @PreAuthorize("hasRole('ADMIN')")
    public UpsertSystemPropertyOverrideResult execute(UpsertSystemPropertyOverrideCommand command) {
        currentAuthProvider.getCurrentAuth();
        SystemPropertyOverride override = propertyService.upsertSystemPropertyOverride(
                resolveOwnerType(command.ownerType()),
                command.propertyKey(),
                command.displayNameOverride(),
                command.displayOrder(),
                command.active()
        );
        return new UpsertSystemPropertyOverrideResult(override.getOwnerType().name(), override.getPropertyKey());
    }

    private PropertyOwnerType resolveOwnerType(String rawOwnerType) {
        try {
            return PropertyOwnerType.valueOf(rawOwnerType);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 owner_type입니다: " + rawOwnerType);
        }
    }
}
