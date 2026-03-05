package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvitationRelationTest {

    @Test
    void invitation_엔티티_입력시_FK와_연관을_동기화한다() {
        User owner = new User("owner@example.com", "hashed", "Owner");
        User inviter = new User("inviter@example.com", "hashed", "Inviter");
        Organization organization = Organization.create(
                "acme",
                "ACME",
                owner.getId(),
                "IT",
                "1-10",
                PlanType.STARTER,
                10,
                1000,
                1024L
        );

        Invitation invitation = Invitation.create(
                organization,
                " invited@example.com ",
                MembershipRole.ADMIN,
                "token-hash",
                inviter,
                Instant.now().plusSeconds(3600)
        );

        assertEquals(organization, invitation.getOrganization());
        assertEquals(organization.getId(), invitation.getOrgId());
        assertEquals(inviter, invitation.getInviter());
        assertEquals(inviter.getId(), invitation.getInvitedBy());
        assertEquals("invited@example.com", invitation.getEmail());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
    }

    @Test
    void invitation_조직이_null이면_예외를_던진다() {
        User inviter = new User("inviter@example.com", "hashed", "Inviter");

        DomainException ex = assertThrows(DomainException.class, () -> Invitation.create(
                null,
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                inviter,
                Instant.now().plusSeconds(3600)
        ));

        assertEquals(Invitation.CODE_INVITATION_ORG_REQUIRED, ex.getDomainCode());
    }

    @Test
    void invitation_이메일이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Invitation.create(
                java.util.UUID.randomUUID(),
                "  ",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(3600)
        ));

        assertEquals(Invitation.CODE_INVITATION_EMAIL_REQUIRED, ex.getDomainCode());
    }
}
