package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.dto.response.CreateOrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.support.CreateOrgContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.dto.request.CreateOrganizationRequest;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateOrganizationUseCase {

    private final AuthTokenParser authTokenParser;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public CreateOrganizationResponse execute(String authorizationHeader, CreateOrganizationRequest request) {
        CreateOrgContext context = authTokenParser.requireCreateOrgToken(authorizationHeader);

        User user = userService.getUserOrThrow(context.userId());
        Organization organization = organizationService.createOrganization(user.getId(), request);

        TokenResponse tokens = jwtTokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                organization.getId(),
                MembershipRole.OWNER.name()
        );

        return new CreateOrganizationResponse(toOrganizationResponse(organization), tokens);
    }

    private OrganizationResponse toOrganizationResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getSlug(),
                organization.getName(),
                organization.getIndustry(),
                organization.getTeamSize(),
                organization.getPlanType().name(),
                fileUrlResolver.resolve(organization.getProfileImageFileKey())
        );
    }
}
