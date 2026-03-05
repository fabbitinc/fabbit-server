package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.AuthAccountService;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.service.input.LoginInput;
import com.fabbitinc.server.application.auth.usecase.command.LoginCommand;
import com.fabbitinc.server.application.auth.usecase.result.AuthTokenResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthUserResult;
import com.fabbitinc.server.application.auth.usecase.result.LoginResult;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class LoginUseCase {

    private final AuthAccountService authAccountService;
    private final OrganizationApi organizationApi;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    public LoginResult execute(LoginCommand command) {
        User user = authAccountService.authenticate(new LoginInput(command.email(), command.password()));
        AuthUserResult userResult = toUserResult(user);
        if (command.slug() == null || command.slug().isBlank()) {
            String scopedToken = jwtTokenService.issueScopedToken(user.getId(), user.getEmail(), "create_org");
            return LoginResult.scoped(userResult, scopedToken);
        }

        Membership membership = organizationApi.switchOrganization(user.getId(), command.slug());
        JwtTokenService.IssuedTokens tokens = jwtTokenService.issueTokenBundle(
                user.getId(),
                user.getEmail(),
                membership.getOrgId(),
                membership.getRole().name()
        );
        return LoginResult.organization(userResult, toTokenResult(tokens));
    }

    private AuthUserResult toUserResult(User user) {
        return new AuthUserResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private AuthTokenResult toTokenResult(JwtTokenService.IssuedTokens issuedTokens) {
        return new AuthTokenResult(
                issuedTokens.accessToken(),
                issuedTokens.refreshToken(),
                issuedTokens.tokenType()
        );
    }
}
