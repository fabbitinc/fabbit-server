package com.fabbitinc.server.application.auth.usecase;

import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.service.JwtTokenService;
import com.fabbitinc.server.application.auth.usecase.command.AcceptInvitationCommand;
import com.fabbitinc.server.application.auth.usecase.result.AcceptInvitationResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthOrganizationResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthTokenResult;
import com.fabbitinc.server.application.auth.usecase.result.AuthUserResult;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AcceptInvitationUseCase {

    private final AuthInvitationService authInvitationService;
    private final UserService userService;
    private final OrganizationApi organizationApi;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    public AcceptInvitationResult execute(AcceptInvitationCommand command) {
        Invitation invitation = authInvitationService.validateInvitationToken(command.token());

        UserService.UserWithNewFlag userWithNewFlag = userService.findOrCreateForInvitation(
                invitation.getEmail(),
                command.password(),
                command.fullName()
        );

        User user = userWithNewFlag.user();
        organizationApi.addMember(user.getId(), invitation.getOrgId(), invitation.getRole());

        invitation.accept(Instant.now());

        Organization organization = authInvitationService.getOrganizationOrThrow(invitation.getOrgId());
        JwtTokenService.IssuedTokens tokens = jwtTokenService.issueTokenBundle(
                user.getId(),
                user.getEmail(),
                invitation.getOrgId(),
                invitation.getRole().name()
        );

        log.atInfo()
                .addKeyValue("event.name", "auth.invitation.accepted")
                .addKeyValue("invitation.id", invitation.getId())
                .addKeyValue("user.id", user.getId())
                .addKeyValue("organization.id", invitation.getOrgId())
                .addKeyValue("outcome", "success")
                .log("invitation accepted");

        return new AcceptInvitationResult(
                toUserResult(user),
                toOrganizationResult(organization),
                toTokenResult(tokens),
                userWithNewFlag.isNewUser()
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
                organization.getPlanType(),
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
