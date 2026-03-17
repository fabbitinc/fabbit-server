package com.fabbitinc.server.domain.aiusage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiUsageLogRelationTest {

    @Test
    void aiUsageEvent_create_입력값을_보관한다() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiUsageEvent event = AiUsageEvent.create(
                orgId,
                userId,
                WorkspacePlanType.TEAM,
                SeatType.FULL,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("1.2500"),
                new BigDecimal("0.00")
        );

        assertEquals(orgId, event.getOrgId());
        assertEquals(userId, event.getUserId());
        assertEquals(WorkspacePlanType.TEAM, event.getPlanTypeSnapshot());
        assertEquals(SeatType.FULL, event.getSeatTypeSnapshot());
        assertEquals(new BigDecimal("1.2500"), event.getCreditsUsed());
    }

    @Test
    void aiUsageEvent_조직이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageEvent.create(
                null,
                UUID.randomUUID(),
                WorkspacePlanType.TEAM,
                SeatType.FULL,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("1.2500"),
                new BigDecimal("0.00")
        ));

        assertEquals(AiUsageEvent.CODE_AI_USAGE_ORG_REQUIRED, ex.getDomainCode());
    }

    @Test
    void aiUsageEvent_credits가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageEvent.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                WorkspacePlanType.TEAM,
                SeatType.FULL,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                null,
                new BigDecimal("0.00")
        ));

        assertEquals(AiUsageEvent.CODE_AI_USAGE_CREDITS_REQUIRED, ex.getDomainCode());
    }

    @Test
    void aiUsageEvent_입력토큰이_음수면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageEvent.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                WorkspacePlanType.TEAM,
                SeatType.FULL,
                "chat",
                "summary",
                "gpt-4.1-mini",
                -1,
                50,
                new BigDecimal("1.2500"),
                new BigDecimal("0.00")
        ));

        assertEquals(AiUsageEvent.CODE_AI_USAGE_INPUT_TOKENS_INVALID, ex.getDomainCode());
    }

    @Test
    void aiUsageEvent_크레딧이_음수면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageEvent.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                WorkspacePlanType.TEAM,
                SeatType.FULL,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("-0.1000"),
                new BigDecimal("0.00")
        ));

        assertEquals(AiUsageEvent.CODE_AI_USAGE_CREDITS_INVALID, ex.getDomainCode());
    }
}
