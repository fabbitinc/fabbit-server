package com.fabbitinc.server.application.organization.query;

import com.fabbitinc.server.application.auth.dto.response.InvitationListResponse;
import com.fabbitinc.server.application.auth.dto.response.InvitationResponse;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.domain.auth.repository.InvitationRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrganizationInvitationQuery {

    private final AuthTokenParser authTokenParser;
    private final InvitationRepository invitationRepository;

    @Transactional(readOnly = true)
    public InvitationListResponse listInvitations(String authorizationHeader) {
        AuthContext auth = authTokenParser.requireRole(authorizationHeader, MembershipRole.ADMIN);

        return new InvitationListResponse(
                invitationRepository.findByOrgIdOrderByCreatedAtDesc(auth.orgId()).stream()
                        .map(invitation -> new InvitationResponse(
                                invitation.getId(),
                                invitation.getOrgId(),
                                invitation.getEmail(),
                                invitation.getRole().name(),
                                invitation.getStatus().name(),
                                invitation.getInvitedBy(),
                                invitation.getExpiresAt(),
                                invitation.getAcceptedAt(),
                                invitation.getCreatedAt()
                        ))
                        .toList()
        );
    }
}
