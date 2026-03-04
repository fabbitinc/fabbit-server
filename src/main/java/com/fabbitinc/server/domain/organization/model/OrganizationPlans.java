package com.fabbitinc.server.domain.organization.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OrganizationPlans {
    private OrganizationPlans() {
    }

    private static final Map<PlanType, PlanLimits> PLAN_LIMITS = createPlanLimits();

    public static Map<PlanType, PlanLimits> limits() {
        return PLAN_LIMITS;
    }

    private static Map<PlanType, PlanLimits> createPlanLimits() {
        LinkedHashMap<PlanType, PlanLimits> limits = new LinkedHashMap<>();
        limits.put(PlanType.STARTER, new PlanLimits(
                5,
                2,
                100,
                0,
                "Starter",
                "소규모 팀이 부담 없이 시작할 수 있는 상시 무료 플랜"
        ));
        limits.put(PlanType.TEAM, new PlanLimits(
                20,
                100,
                3_000,
                249_000,
                "Team",
                "실무 운영에 맞춘 중소 제조팀 기본 플랜"
        ));
        limits.put(PlanType.BUSINESS, new PlanLimits(
                50,
                500,
                10_000,
                599_000,
                "Business",
                "부서 간 협업과 대량 처리가 필요한 조직용 플랜"
        ));
        limits.put(PlanType.ENTERPRISE, new PlanLimits(
                -1,
                2_000,
                50_000,
                -1,
                "Enterprise",
                "전사 도입과 맞춤 운영이 필요한 대규모 조직용 플랜"
        ));
        return Map.copyOf(limits);
    }
}
