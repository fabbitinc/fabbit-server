package com.fabbitinc.server.domain.subscription.model;

import java.math.BigDecimal;

public enum WorkspacePlanType {
    STARTER(
            "Starter",
            "최대 5명까지 무료로 시작하는 플랜",
            5,
            250_000_000L,
            0L,
            100,
            AiBillingMode.INCLUDED_ONLY,
            0,
            0,
            0
    ),
    TEAM(
            "Team",
            "좌석 기반으로 운영하는 기본 유료 플랜",
            -1,
            10_000_000_000L,
            10_000_000_000L,
            0,
            AiBillingMode.METERED,
            5_000,
            15_000,
            29_000
    ),
    ORG(
            "Org",
            "조직 단위 운영과 확장에 맞춘 플랜",
            -1,
            100_000_000_000L,
            50_000_000_000L,
            0,
            AiBillingMode.METERED,
            5_000,
            25_000,
            59_000
    ),
    ENTERPRISE(
            "Enterprise",
            "맞춤 계약과 대규모 운영을 위한 플랜",
            -1,
            100_000_000_000L,
            50_000_000_000L,
            0,
            AiBillingMode.METERED,
            5_000,
            25_000,
            59_000
    );

    public static final long STORAGE_OVERAGE_UNIT_BYTES = 1_000_000_000L;
    public static final BigDecimal STORAGE_OVERAGE_UNIT_PRICE = BigDecimal.valueOf(200L);

    private final String displayName;
    private final String description;
    private final int maxMembers;
    private final long baseStorageBytes;
    private final long extraStorageBytesPerFullSeat;
    private final int starterMonthlyAiCredits;
    private final AiBillingMode aiBillingMode;
    private final int viewerMonthlyPrice;
    private final int collaboratorMonthlyPrice;
    private final int fullSeatMonthlyPrice;

    WorkspacePlanType(
            String displayName,
            String description,
            int maxMembers,
            long baseStorageBytes,
            long extraStorageBytesPerFullSeat,
            int starterMonthlyAiCredits,
            AiBillingMode aiBillingMode,
            int viewerMonthlyPrice,
            int collaboratorMonthlyPrice,
            int fullSeatMonthlyPrice
    ) {
        this.displayName = displayName;
        this.description = description;
        this.maxMembers = maxMembers;
        this.baseStorageBytes = baseStorageBytes;
        this.extraStorageBytesPerFullSeat = extraStorageBytesPerFullSeat;
        this.starterMonthlyAiCredits = starterMonthlyAiCredits;
        this.aiBillingMode = aiBillingMode;
        this.viewerMonthlyPrice = viewerMonthlyPrice;
        this.collaboratorMonthlyPrice = collaboratorMonthlyPrice;
        this.fullSeatMonthlyPrice = fullSeatMonthlyPrice;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int maxMembers() {
        return maxMembers;
    }

    public long baseStorageBytes() {
        return baseStorageBytes;
    }

    public long extraStorageBytesPerFullSeat() {
        return extraStorageBytesPerFullSeat;
    }

    public int starterMonthlyAiCredits() {
        return starterMonthlyAiCredits;
    }

    public AiBillingMode aiBillingMode() {
        return aiBillingMode;
    }

    public int viewerMonthlyPrice() {
        return viewerMonthlyPrice;
    }

    public int collaboratorMonthlyPrice() {
        return collaboratorMonthlyPrice;
    }

    public int fullSeatMonthlyPrice() {
        return fullSeatMonthlyPrice;
    }

    public boolean isStarter() {
        return this == STARTER;
    }

    public boolean allowsStorageOverage() {
        return this != STARTER;
    }

    public int seatPrice(SeatType seatType) {
        return switch (seatType) {
            case STARTER -> 0;
            case VIEWER -> viewerMonthlyPrice;
            case COLLABORATOR -> collaboratorMonthlyPrice;
            case FULL -> fullSeatMonthlyPrice;
        };
    }
}
