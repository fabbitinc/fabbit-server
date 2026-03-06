package com.fabbitinc.server.domain.aiusage.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiUsageLogRelationTest {

    @Test
    void aiUsageLog_create_입력값을_보관한다() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiUsageLog log = AiUsageLog.create(
                orgId,
                userId,
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("1.2500")
        );

        assertEquals(orgId, log.getOrgId());
        assertEquals(userId, log.getUserId());
        assertEquals(new BigDecimal("1.2500"), log.getCreditsUsed());
    }

    @Test
    void aiUsageLog_조직이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageLog.create(
                null,
                UUID.randomUUID(),
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

    @Test
    void aiUsageLog_입력토큰이_음수면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageLog.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "chat",
                "summary",
                "gpt-4.1-mini",
                -1,
                50,
                new BigDecimal("1.2500")
        ));

        assertEquals(AiUsageLog.CODE_AI_USAGE_INPUT_TOKENS_INVALID, ex.getDomainCode());
    }

    @Test
    void aiUsageLog_크레딧이_음수면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> AiUsageLog.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "chat",
                "summary",
                "gpt-4.1-mini",
                100,
                50,
                new BigDecimal("-0.1000")
        ));

        assertEquals(AiUsageLog.CODE_AI_USAGE_CREDITS_INVALID, ex.getDomainCode());
    }
}
