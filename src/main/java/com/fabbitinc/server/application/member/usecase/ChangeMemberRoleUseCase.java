package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
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

    private final AuthTokenParser authTokenParser;
    private final OrganizationService organizationService;

    @Transactional
    public void execute(String authorizationHeader, UUID userId, ChangeRoleRequest request) {
        AuthContext auth = authTokenParser.requireRole(authorizationHeader, MembershipRole.OWNER);
        organizationService.changeMemberRole(auth, userId, request.role());
    }
}
