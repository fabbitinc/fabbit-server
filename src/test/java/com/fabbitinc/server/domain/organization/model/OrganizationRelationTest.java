package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationRelationTest {

    @Test
    void organization_기본_멤버십_컬렉션은_비어있다() {
        Organization organization = Organization.create(
                "acme",
                "ACME",
                java.util.UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );

        assertTrue(organization.getMemberships().isEmpty());
    }

    @Test
    void membership_엔티티_입력시_FK와_연관을_동기화한다() {
        User user = new User("member@acme.com", "hashed", "Member");
        Organization organization = Organization.create(
                "acme",
                "ACME",
                user.getId(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );

        Membership membership = Membership.create(user, organization, MembershipRole.ADMIN, "QA");

        assertEquals(user, membership.getUser());
        assertEquals(organization, membership.getOrganization());
        assertEquals(user.getId(), membership.getUserId());
        assertEquals(organization.getId(), membership.getOrgId());
        assertEquals(MembershipRole.ADMIN, membership.getRole());
    }

    @Test
    void membership_changeRole_null이면_예외를_던진다() {
        Membership membership = Membership.create(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                MembershipRole.MEMBER,
                null
        );

        DomainException ex = assertThrows(DomainException.class, () -> membership.changeRole(null));

        assertEquals(Membership.CODE_MEMBERSHIP_ROLE_REQUIRED, ex.getDomainCode());
    }
}
