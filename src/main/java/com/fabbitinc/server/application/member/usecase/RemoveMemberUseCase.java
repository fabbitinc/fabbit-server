package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.member.usecase.command.RemoveMemberCommand;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RemoveMemberUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(RemoveMemberCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        organizationApi.removeMember(auth, command.userId());
        log.atInfo()
                .addKeyValue("event.name", "member.removed")
                .addKeyValue("organization.id", auth.orgId())
                .addKeyValue("actor.user.id", auth.userId())
                .addKeyValue("target.user.id", command.userId())
                .addKeyValue("outcome", "success")
                .log("member removed");
    }
}
