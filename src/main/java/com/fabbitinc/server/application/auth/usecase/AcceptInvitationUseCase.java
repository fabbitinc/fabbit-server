package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.dto.request.AcceptInvitationRequest;
import com.fabbitinc.server.application.auth.dto.response.AcceptInvitationResponse;
import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AcceptInvitationUseCase {

    private final AuthInvitationService authInvitationService;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public AcceptInvitationResponse execute(AcceptInvitationRequest request) {
        Invitation invitation = authInvitationService.validateInvitationToken(request.token());

        UserService.UserWithNewFlag userWithNewFlag = userService.findOrCreateForInvitation(
                invitation.getEmail(),
                request.password(),
                request.fullName()
        );

        User user = userWithNewFlag.user();
        organizationService.addMember(user.getId(), invitation.getOrgId(), invitation.getRole());

        invitation.accept(Instant.now());

        Organization organization = authInvitationService.getOrganizationOrThrow(invitation.getOrgId());
        TokenResponse tokens = jwtTokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                invitation.getOrgId(),
                invitation.getRole().name()
        );

        return new AcceptInvitationResponse(
                toUserResponse(user),
                toOrganizationResponse(organization),
                tokens,
                userWithNewFlag.isNewUser()
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
