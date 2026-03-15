package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.support.CreateOrgContext;
import com.fabbitinc.server.application.auth.support.CurrentCreateOrgProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.service.input.CreateOrganizationInput;
import com.fabbitinc.server.application.organization.usecase.command.CreateOrganizationCommand;
import com.fabbitinc.server.application.organization.usecase.result.CreateOrganizationResult;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CreateOrganizationUseCase {

    private final CurrentCreateOrgProvider currentCreateOrgProvider;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    public CreateOrganizationResult execute(CreateOrganizationCommand command) {
        CreateOrgContext context = currentCreateOrgProvider.getCurrentCreateOrg();

        User user = userService.getUserOrThrow(context.userId());
        Organization organization = organizationService.createWorkspace(
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

        return new CreateOrganizationResult(
                organization.getId(),
                organization.getSlug(),
                organization.getName(),
                organization.getIndustry(),
                organization.getTeamSize(),
                organization.getPlanType(),
                fileUrlResolver.resolve(organization.getProfileImageFileKey()),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType()
        );
    }
}
