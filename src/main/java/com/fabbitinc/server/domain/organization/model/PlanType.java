package com.fabbitinc.server.domain.organization.model;

/**

    - 5명: 설계팀만 쓰는 최소 단위. "일단 써보자"의 진입점
    - 20명: 설계 + 구매 + 생산 핵심 인력. 제안하신 10명은 제조업에서 약간 빡빡함 — 설계 5 + 구매 3 + 생산 2면 바로 찬다
    - 50명: 30명도 괜찮지만 50명이면 "넉넉하다" 느낌을 줘서 업셀 시점을 늦출 수 있음. 대신 Business 가격을 그만큼 높게 잡으면 됨
    - 무제한: Enterprise는 어차피 견적이니까 숫자 의미 없음

    ┌────────────────┬───────────────────────────────────┬───────────────────────────────┐
    │      기능       │        왜 차등이 타당한가         │             제한              │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ 인원            │ 서버 비용 비례                    │ 5 / 20 / 50 / 무제한          │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ 스토리지         │ 저장 비용 비례                    │ 2 / 100 / 500 / 2,000 GB      │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ AI 크레딧        │ 추론 비용 비례                    │ 100 / 3,000 / 10,000 / 50,000 │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ SSO/SAML       │ 중견 이상만 필요, 구현 비용 있음  │ Enterprise만                  │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ 감사 로그 보관     │ 저장 비용 + 컴플라이언스          │ 30일 / 1년 / 무제한           │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ API 접근        │ 연동 자동화는 규모 있는 조직 니즈 │ Business+                     │
    ├────────────────┼───────────────────────────────────┼───────────────────────────────┤
    │ 전담 지원        │ 인건비                            │ Enterprise만                  │
    └────────────────┴───────────────────────────────────┴───────────────────────────────┘


    ┌──────┬──────────────────┬────────────────────────────────────────────────────────────┐
    │ 단계 │       시점       │                            대응                            │
    ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
    │ 지금 │ 출시 전          │ 무시. SaaS에 집중                                          │
    ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
    │ 초기 │ ~100개사         │ "Enterprise 플랜에서 검토 중"으로 응대                     │
    ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
    │ 중기 │ 100개사+         │ VPC 배포 (고객 AWS 계정에 설치) — 온프레미스보다 훨씬 쉬움 │
    ├──────┼──────────────────┼────────────────────────────────────────────────────────────┤
    │ 후기 │ 대기업 계약 확보 │ 진짜 온프레미스 or 하이브리드                              │
    └──────┴──────────────────┴────────────────────────────────────────────────────────────┘
 */

public enum PlanType {
    STARTER(
            5,
            2,
            100,
            0,
            "Starter",
            "소규모 팀이 부담 없이 시작할 수 있는 상시 무료 플랜"
    ),
    TEAM(
            20,
            100,
            3_000,
            249_000,
            "Team",
            "실무 운영에 맞춘 중소 제조팀 기본 플랜"
    ),
    BUSINESS(
            50,
            500,
            10_000,
            599_000,
            "Business",
            "부서 간 협업과 대량 처리가 필요한 조직용 플랜"
    ),
    ENTERPRISE(
            -1,
            2_000,
            50_000,
            -1,
            "Enterprise",
            "전사 도입과 맞춤 운영이 필요한 대규모 조직용 플랜"
    );

    private final int maxMembers;
    private final int storageGb;
    private final int aiCredits;
    private final int priceMonthly;
    private final String displayName;
    private final String description;

    PlanType(
            int maxMembers,
            int storageGb,
            int aiCredits,
            int priceMonthly,
            String displayName,
            String description
    ) {
        this.maxMembers = maxMembers;
        this.storageGb = storageGb;
        this.aiCredits = aiCredits;
        this.priceMonthly = priceMonthly;
        this.displayName = displayName;
        this.description = description;
    }

    public int maxMembers() {
        return maxMembers;
    }

    public int storageGb() {
        return storageGb;
    }

    public int aiCredits() {
        return aiCredits;
    }

    public int priceMonthly() {
        return priceMonthly;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static PlanType defaultIfNull(PlanType planType) {
        return planType == null ? STARTER : planType;
    }
}
