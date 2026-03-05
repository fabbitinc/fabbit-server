package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.usecase.command.SwitchOrganizationCommand;
import com.fabbitinc.server.application.organization.usecase.result.SwitchOrganizationResult;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class SwitchOrganizationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    public SwitchOrganizationResult execute(SwitchOrganizationCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Membership membership = organizationService.switchOrganization(auth.userId(), command.slug());
        User user = userService.getUserOrThrow(auth.userId());

        JwtTokenService.IssuedTokens tokens = jwtTokenService.issueTokenBundle(
                user.getId(),
                user.getEmail(),
                membership.getOrgId(),
                membership.getRole().name()
        );

        return new SwitchOrganizationResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.isActive(),
                user.getCreatedAt(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType()
        );
    }
}
