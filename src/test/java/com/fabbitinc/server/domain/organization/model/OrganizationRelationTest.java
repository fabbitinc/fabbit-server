package com.fabbitinc.server.domain.organization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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
    void organization_addMember_사용자_ID로_멤버를_추가한다() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Organization organization = Organization.create(
                "acme",
                "ACME",
                ownerId,
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );

        Membership membership = organization.addMember(memberId, MembershipRole.ADMIN, "QA");

        assertEquals(organization, membership.getOrganization());
        assertEquals(memberId, membership.getUserId());
        assertEquals(organization.getId(), membership.getOrgId());
        assertEquals(MembershipRole.ADMIN, membership.getRole());
        assertEquals(1, organization.getMemberships().size());
    }

    @Test
    void membership_changeRole_null이면_예외를_던진다() {
        Organization organization = Organization.create(
                "acme",
                "ACME",
                UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );
        Membership membership = Membership.create(
                organization,
                UUID.randomUUID(),
                MembershipRole.MEMBER,
                null
        );

        DomainException ex = assertThrows(DomainException.class, () -> organization.changeMemberRole(membership, null, 2));

        assertEquals(Membership.CODE_MEMBERSHIP_ROLE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void membership_jobRole은_trim_정규화한다() {
        Membership membership = Membership.create(
                Organization.create(
                        "acme",
                        "ACME",
                        UUID.randomUUID(),
                        "manufacturing",
                        "11-50",
                        PlanType.STARTER,
                        10,
                        1000,
                        1_000_000L
                ),
                UUID.randomUUID(),
                MembershipRole.MEMBER,
                "  QA Engineer  "
        );

        assertEquals("QA Engineer", membership.getJobRole());
    }

    @Test
    void membership_jobRole이_blank면_null로_정규화한다() {
        Membership membership = Membership.create(
                Organization.create(
                        "acme",
                        "ACME",
                        UUID.randomUUID(),
                        "manufacturing",
                        "11-50",
                        PlanType.STARTER,
                        10,
                        1000,
                        1_000_000L
                ),
                UUID.randomUUID(),
                MembershipRole.MEMBER,
                "   "
        );

        assertNull(membership.getJobRole());
    }

    @Test
    void membership_jobRole이_너무_길면_예외를_던진다() {
        String tooLong = "a".repeat(51);

        DomainException ex = assertThrows(DomainException.class, () -> Membership.create(
                Organization.create(
                        "acme",
                        "ACME",
                        UUID.randomUUID(),
                        "manufacturing",
                        "11-50",
                        PlanType.STARTER,
                        10,
                        1000,
                        1_000_000L
                ),
                UUID.randomUUID(),
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

    @Test
    void organization_reserveMemberSeat_한도초과면_예외를_던진다() {
        Organization organization = Organization.create(
                "acme",
                "ACME",
                UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                1,
                1000,
                1_000_000L
        );
        organization.reserveMemberSeat();

        DomainException ex = assertThrows(DomainException.class, organization::reserveMemberSeat);

        assertEquals(Organization.CODE_ORGANIZATION_MEMBER_LIMIT_EXCEEDED, ex.getDomainCode());
    }

    @Test
    void organization_useCredits는_plan부터_차감하고_부족분은_bonus에서_차감한다() {
        Organization organization = Organization.create(
                "acme",
                "ACME",
                UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );
        organization.grantBonusCredits(500);

        organization.useCredits(1200);

        assertEquals(0, organization.getPlanCreditsRemaining());
        assertEquals(300, organization.getBonusCreditsRemaining());
    }

    @Test
    void organization_addStorageUsage_한도초과면_예외를_던진다() {
        Organization organization = Organization.create(
                "acme",
                "ACME",
                UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                100L
        );

        DomainException ex = assertThrows(DomainException.class, () -> organization.addStorageUsage(101L));

        assertEquals(Organization.CODE_ORGANIZATION_STORAGE_LIMIT_EXCEEDED, ex.getDomainCode());
    }

    @Test
    void organization_changeMemberRole_마지막_owner는_강등할수없다() {
        Organization organization = Organization.create(
                "acme",
                "ACME",
                UUID.randomUUID(),
                "manufacturing",
                "11-50",
                PlanType.STARTER,
                10,
                1000,
                1_000_000L
        );
        Membership owner = organization.addMember(UUID.randomUUID(), MembershipRole.OWNER, null);

        DomainException ex = assertThrows(
                DomainException.class,
                () -> organization.changeMemberRole(owner, MembershipRole.ADMIN, 1)
        );

        assertEquals(Organization.CODE_ORGANIZATION_LAST_OWNER_ROLE_CHANGE_FORBIDDEN, ex.getDomainCode());
    }
}
