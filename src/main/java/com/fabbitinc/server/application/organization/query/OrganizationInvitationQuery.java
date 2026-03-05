package com.fabbitinc.server.application.organization.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.organization.query.result.OrganizationInvitationListResult;
import com.fabbitinc.server.application.organization.query.result.OrganizationInvitationResult;
import com.fabbitinc.server.domain.auth.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationInvitationQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final InvitationRepository invitationRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public OrganizationInvitationListResult listInvitations() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        return new OrganizationInvitationListResult(
                invitationRepository.findByOrgIdOrderByCreatedAtDesc(auth.orgId()).stream()
                        .map(invitation -> new OrganizationInvitationResult(
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
