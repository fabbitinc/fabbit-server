package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.organization.usecase.command.CancelInvitationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CancelInvitationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final AuthInvitationService authInvitationService;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(CancelInvitationCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        authInvitationService.cancelInvitation(auth.orgId(), command.invitationId());
    }
}
