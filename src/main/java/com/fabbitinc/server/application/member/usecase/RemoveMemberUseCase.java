package com.fabbitinc.server.application.member.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RemoveMemberUseCase {

    private final AuthTokenParser authTokenParser;
    private final OrganizationService organizationService;

    @Transactional
    public void execute(String authorizationHeader, UUID userId) {
        AuthContext auth = authTokenParser.requireAdmin(authorizationHeader);
        organizationService.removeMember(auth, userId);
    }
}
