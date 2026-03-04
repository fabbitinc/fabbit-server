package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.dto.response.LoginResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.dto.request.SwitchOrgRequest;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SwitchOrganizationUseCase {

    private final AuthTokenParser authTokenParser;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public LoginResponse execute(String authorizationHeader, SwitchOrgRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        Membership membership = organizationService.switchOrganization(auth.userId(), request.slug());
        User user = userService.getUserOrThrow(auth.userId());

        TokenResponse tokens = jwtTokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                membership.getOrgId(),
                membership.getRole().name()
        );

        return new LoginResponse(toUserResponse(user), tokens);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
