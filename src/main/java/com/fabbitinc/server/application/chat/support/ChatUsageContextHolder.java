package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import java.util.UUID;

/**
 * LLM 호출 시 비즈니스 컨텍스트(org, user, category, feature)를 ThreadLocal로 전달합니다.
 * ObservationHandler에서 이 컨텍스트를 읽어 usage 기록 및 크레딧 차감에 사용합니다.
 */
public final class ChatUsageContextHolder {

    private static final ThreadLocal<UsageContext> HOLDER = new ThreadLocal<>();

    private ChatUsageContextHolder() {
    }

    public static void set(UUID orgId, UUID userId, AiUsageCategory category, String feature) {
        HOLDER.set(new UsageContext(orgId, userId, category, feature));
    }

    public static UsageContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record UsageContext(
            UUID orgId,
            UUID userId,
            AiUsageCategory category,
            String feature
    ) {
    }
}
