package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.member.dto.request.ChangeRoleRequest;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChangeMemberRoleUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationService organizationService;

    @Transactional
    public void execute(UUID userId, ChangeRoleRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth(MembershipRole.OWNER);
        organizationService.changeMemberRole(auth, userId, request.role());
    }
}
