package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.dto.request.CreateInvitationRequest;
import com.fabbitinc.server.application.auth.dto.response.InvitationResponse;
import com.fabbitinc.server.application.auth.service.AuthInvitationService;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateInvitationUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final AuthInvitationService authInvitationService;

    @Transactional
    public InvitationResponse execute(CreateInvitationRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth(MembershipRole.ADMIN);

        userService.getUserByEmail(request.email())
                .ifPresent(existingUser -> organizationService.checkNotMember(auth.orgId(), existingUser.getId()));

        AuthInvitationService.CreatedInvitation created = authInvitationService.createInvitationRecord(
                auth.orgId(),
                request.email(),
                auth.userId(),
                request.role(),
                auth.role()
        );

        Organization organization = authInvitationService.getOrganizationOrThrow(auth.orgId());
        User inviter = authInvitationService.getInviterOrThrow(auth.userId());
        String inviteUrl = authInvitationService.buildInviteUrl(created.rawToken(), organization.getSlug());
        authInvitationService.sendInvitationEmail(request.email(), organization.getName(), inviter.getFullName(), inviteUrl);

        return toResponse(created.invitation());
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getOrgId(),
                invitation.getEmail(),
                invitation.getRole().name(),
                invitation.getStatus().name(),
                invitation.getInvitedBy(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getCreatedAt()
        );
    }
}
