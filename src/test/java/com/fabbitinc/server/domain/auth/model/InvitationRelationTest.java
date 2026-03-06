package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.PlanType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationRelationTest {

    @Test
    void invitation_create_입력값을_정규화한다() {
        java.util.UUID orgId = java.util.UUID.randomUUID();
        java.util.UUID inviterId = java.util.UUID.randomUUID();

        Invitation invitation = Invitation.create(
                orgId,
                " invited@example.com ",
                MembershipRole.ADMIN,
                "token-hash",
                inviterId,
                Instant.now().plusSeconds(3600)
        );

        assertEquals(orgId, invitation.getOrgId());
        assertEquals(inviterId, invitation.getInvitedBy());
        assertEquals("invited@example.com", invitation.getEmail());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
    }

    @Test
    void invitation_조직이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Invitation.create(
                null,
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
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

    @Test
    void invitation_accept_대기상태에서_수락하면_상태와_수락시각을_설정한다() {
        Invitation invitation = Invitation.create(
                java.util.UUID.randomUUID(),
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(3600)
        );
        Instant now = Instant.now();

        invitation.accept(now);

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        assertEquals(now, invitation.getAcceptedAt());
    }

    @Test
    void invitation_accept_now가_null이면_예외를_던지고_상태를_유지한다() {
        Invitation invitation = Invitation.create(
                java.util.UUID.randomUUID(),
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(3600)
        );

        DomainException ex = assertThrows(DomainException.class, () -> invitation.accept(null));

        assertEquals(Invitation.CODE_INVITATION_TIME_REQUIRED, ex.getDomainCode());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertNull(invitation.getAcceptedAt());
    }

    @Test
    void invitation_accept_만료된_초대면_예외를_던진다() {
        Invitation invitation = Invitation.create(
                java.util.UUID.randomUUID(),
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(1)
        );

        DomainException ex = assertThrows(DomainException.class, () -> invitation.accept(Instant.now().plusSeconds(2)));

        assertEquals(Invitation.CODE_INVITATION_INVALID_STATE, ex.getDomainCode());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertNull(invitation.getAcceptedAt());
    }

    @Test
    void invitation_cancel_처리된_초대면_예외를_던진다() {
        Invitation invitation = Invitation.create(
                java.util.UUID.randomUUID(),
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(3600)
        );
        invitation.accept(Instant.now());

        DomainException ex = assertThrows(DomainException.class, invitation::cancel);

        assertEquals(Invitation.CODE_INVITATION_INVALID_STATE, ex.getDomainCode());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
    }

    @Test
    void invitation_isExpired_now가_null이면_예외를_던진다() {
        Invitation invitation = Invitation.create(
                java.util.UUID.randomUUID(),
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(3600)
        );

        DomainException ex = assertThrows(DomainException.class, () -> invitation.isExpired(null));

        assertEquals(Invitation.CODE_INVITATION_TIME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void invitation_isExpired_만료여부를_판단한다() {
        Invitation invitation = Invitation.create(
                java.util.UUID.randomUUID(),
                "invited@example.com",
                MembershipRole.ADMIN,
                "token-hash",
                java.util.UUID.randomUUID(),
                Instant.now().plusSeconds(10)
        );

        assertFalse(invitation.isExpired(Instant.now()));
        assertTrue(invitation.isExpired(Instant.now().plusSeconds(11)));
    }
}
