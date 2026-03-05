package com.fabbitinc.server.domain.aiusage.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiUsageLogRelationTest {

    @Test
    void aiUsageLog_엔티티_입력시_org_user_FK와_연관을_동기화한다() {
        User owner = new User("owner@example.com", "hashed", "Owner");
        User user = new User("user@example.com", "hashed", "User");
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

        AiUsageLog log = AiUsageLog.create(
                organization,
                user,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("1.2500")
        );

        assertEquals(organization, log.getOrganization());
        assertEquals(organization.getId(), log.getOrgId());
        assertEquals(user, log.getUser());
        assertEquals(user.getId(), log.getUserId());
        assertEquals(new BigDecimal("1.2500"), log.getCreditsUsed());
    }

    @Test
    void aiUsageLog_조직이_null이면_예외를_던진다() {
        User user = new User("user@example.com", "hashed", "User");

        DomainException ex = assertThrows(DomainException.class, () -> AiUsageLog.create(
                null,
                user,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("1.2500")
        ));

        assertEquals(AiUsageLog.CODE_AI_USAGE_ORG_REQUIRED, ex.getDomainCode());
    }

    @Test
    void aiUsageLog_credits가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageLog.create(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                null
        ));

        assertEquals(AiUsageLog.CODE_AI_USAGE_CREDITS_REQUIRED, ex.getDomainCode());
    }
}
