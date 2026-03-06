package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationRelationTest {

    @Test
    void organization_기본_멤버십_컬렉션은_비어있다() {
        Organization organization = Organization.create(
                " acme ",
                " ACME ",
                java.util.UUID.randomUUID(),
                " manufacturing ",
                " 11-50 ",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );

        assertEquals("acme", organization.getSlug());
        assertEquals("ACME", organization.getName());
        assertEquals("manufacturing", organization.getIndustry());
        assertEquals("11-50", organization.getTeamSize());
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

    @Test
    void membership_jobRole은_trim_정규화한다() {
        Membership membership = Membership.create(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                MembershipRole.MEMBER,
                "  QA Engineer  "
        );

        assertEquals("QA Engineer", membership.getJobRole());
    }

    @Test
    void membership_jobRole이_blank면_null로_정규화한다() {
        Membership membership = Membership.create(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                MembershipRole.MEMBER,
                "   "
        );

        assertNull(membership.getJobRole());
    }

    @Test
    void membership_jobRole이_너무_길면_예외를_던진다() {
        String tooLong = "a".repeat(51);

        DomainException ex = assertThrows(DomainException.class, () -> Membership.create(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                MembershipRole.MEMBER,
                tooLong
        ));

        assertEquals(Membership.CODE_MEMBERSHIP_JOB_ROLE_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void organization_slug가_blank면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Organization.create(
                "   ",
                "ACME",
                java.util.UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        ));

        assertEquals(Organization.CODE_ORGANIZATION_SLUG_REQUIRED, ex.getDomainCode());
    }

    @Test
    void organization_rename_blank면_예외를_던진다() {
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

        DomainException ex = assertThrows(DomainException.class, () -> organization.rename("   "));

        assertEquals(Organization.CODE_ORGANIZATION_NAME_REQUIRED, ex.getDomainCode());
        assertEquals("ACME", organization.getName());
    }

    @Test
    void organization_changeProfileImage_blank면_null로_정규화한다() {
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

        organization.changeProfileImage("   ");

        assertNull(organization.getProfileImageFileKey());
    }
}
