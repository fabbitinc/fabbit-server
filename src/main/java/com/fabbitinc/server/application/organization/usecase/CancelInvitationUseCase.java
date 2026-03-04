package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CancelInvitationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final AuthInvitationService authInvitationService;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UUID invitationId) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        authInvitationService.cancelInvitation(auth.orgId(), invitationId);
    }
}
