package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.AuthAccountService;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.service.input.RegisterUserInput;
import com.fabbitinc.server.application.auth.usecase.command.RegisterCommand;
import com.fabbitinc.server.application.auth.usecase.result.AuthOrganizationResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthTokenResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthUserResult;
import com.fabbitinc.server.application.auth.usecase.result.RegisterResult;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RegisterUseCase {

    private final AuthAccountService authAccountService;
    private final OrganizationApi organizationApi;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    public RegisterResult execute(RegisterCommand command) {
        User user = authAccountService.registerUser(
                new RegisterUserInput(
                        command.verificationToken(),
                        command.code(),
                        command.password(),
                        command.fullName()
                )
        );
        Organization organization = organizationApi.createOrganization(
                user.getId(),
                new CreateOrganizationInput(
                        command.orgName(),
                        command.slug(),
                        command.industry(),
                        command.teamSize(),
                        command.planType()
                )
        );

        JwtTokenService.IssuedTokens tokens = jwtTokenService.issueTokenBundle(
                user.getId(),
                user.getEmail(),
                organization.getId(),
                MembershipRole.OWNER.name()
        );

        return new RegisterResult(
                toUserResult(user),
                toOrganizationResult(organization),
                toTokenResult(tokens)
        );
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

    private AuthOrganizationResult toOrganizationResult(Organization organization) {
        return new AuthOrganizationResult(
                organization.getId(),
                organization.getSlug(),
                organization.getName(),
                organization.getIndustry(),
                organization.getTeamSize(),
                organization.getPlanType().name(),
                fileUrlResolver.resolve(organization.getProfileImageFileKey())
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
