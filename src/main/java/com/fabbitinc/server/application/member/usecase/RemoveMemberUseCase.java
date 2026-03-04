package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RemoveMemberUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationService organizationService;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UUID userId) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        organizationService.removeMember(auth, userId);
    }
}
