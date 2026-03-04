package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.member.dto.request.ChangeRoleRequest;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChangeMemberRoleUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationService organizationService;

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public void execute(UUID userId, ChangeRoleRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        organizationService.changeMemberRole(auth, userId, request.role());
    }
}
