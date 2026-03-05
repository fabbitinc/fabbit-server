package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.RegisterRequest;
import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.RegisterResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.auth.service.AuthAccountService;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
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

    public RegisterResponse execute(RegisterRequest request) {
        User user = authAccountService.registerUser(request);
        Organization organization = organizationApi.createOrganization(
                user.getId(),
                new CreateOrganizationInput(
                        request.orgName(),
                        request.slug(),
                        request.industry(),
                        request.teamSize(),
                        request.planType()
                )
        );

        TokenResponse tokens = jwtTokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                organization.getId(),
                MembershipRole.OWNER.name()
        );

        return new RegisterResponse(
                toUserResponse(user),
                toOrganizationResponse(organization),
                tokens
        );
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
