package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionRelationTest {

    @Test
    void subscription_엔티티_입력시_org_FK와_연관을_동기화한다() {
        User owner = new User("owner@example.com", "hashed", "Owner");
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
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        Subscription subscription = Subscription.create(
                organization,
                "STARTER",
                SubscriptionStatus.ACTIVE,
                start,
                end,
                10,
                1000,
                1024L
        );

        assertEquals(organization, subscription.getOrganization());
        assertEquals(organization.getId(), subscription.getOrgId());
        assertEquals("STARTER", subscription.getPlanType());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    void subscription_조직이_null이면_예외를_던진다() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                (Organization) null,
                "STARTER",
                SubscriptionStatus.ACTIVE,
                start,
                end,
                10,
                1000,
                1024L
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_ORG_REQUIRED, ex.getDomainCode());
    }

    @Test
    void subscription_planType이_비어있으면_예외를_던진다() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                java.util.UUID.randomUUID(),
                " ",
                SubscriptionStatus.ACTIVE,
                start,
                end,
                10,
                1000,
                1024L
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED, ex.getDomainCode());
    }
}
