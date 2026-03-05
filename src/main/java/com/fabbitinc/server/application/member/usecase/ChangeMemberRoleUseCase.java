package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.member.usecase.command.ChangeMemberRoleCommand;
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
public class ChangeMemberRoleUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;

    @PreAuthorize("hasRole('OWNER')")
    public void execute(ChangeMemberRoleCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        organizationApi.changeMemberRole(auth, command.userId(), command.role());
        log.atInfo()
                .addKeyValue("event.name", "member.role.changed")
                .addKeyValue("organization.id", auth.orgId())
                .addKeyValue("actor.user.id", auth.userId())
                .addKeyValue("target.user.id", command.userId())
                .addKeyValue("new.role", command.role())
                .addKeyValue("outcome", "success")
                .log("member role changed");
    }
}
