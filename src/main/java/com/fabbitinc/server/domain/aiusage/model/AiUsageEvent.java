package com.fabbitinc.server.domain.aiusage.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "ai_usage_events",
        indexes = {
                @Index(name = "ix_ai_usage_events_org_id", columnList = "org_id"),
                @Index(name = "ix_ai_usage_events_user_id", columnList = "user_id"),
                @Index(name = "ix_ai_usage_events_org_id_created_at", columnList = "org_id,created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsageEvent extends AbstractCreatedEntity {

    public static final String BILLING_STATUS_PENDING = "PENDING";
    public static final String BILLING_STATUS_BILLED = "BILLED";

    public static final String CODE_AI_USAGE_ORGANIZATION_REQUIRED = "AI_USAGE_ORGANIZATION_REQUIRED";
    public static final String CODE_AI_USAGE_USER_REQUIRED = "AI_USAGE_USER_REQUIRED";
    public static final String CODE_AI_USAGE_PLAN_TYPE_REQUIRED = "AI_USAGE_PLAN_TYPE_REQUIRED";
    public static final String CODE_AI_USAGE_SEAT_TYPE_REQUIRED = "AI_USAGE_SEAT_TYPE_REQUIRED";
    public static final String CODE_AI_USAGE_CATEGORY_REQUIRED = "AI_USAGE_CATEGORY_REQUIRED";
    public static final String CODE_AI_USAGE_FEATURE_REQUIRED = "AI_USAGE_FEATURE_REQUIRED";
    public static final String CODE_AI_USAGE_MODEL_REQUIRED = "AI_USAGE_MODEL_REQUIRED";
    public static final String CODE_AI_USAGE_CREDITS_REQUIRED = "AI_USAGE_CREDITS_REQUIRED";
    public static final String CODE_AI_USAGE_INPUT_TOKENS_INVALID = "AI_USAGE_INPUT_TOKENS_INVALID";
    public static final String CODE_AI_USAGE_OUTPUT_TOKENS_INVALID = "AI_USAGE_OUTPUT_TOKENS_INVALID";
    public static final String CODE_AI_USAGE_CREDITS_INVALID = "AI_USAGE_CREDITS_INVALID";
    public static final String CODE_AI_USAGE_BILLABLE_AMOUNT_INVALID = "AI_USAGE_BILLABLE_AMOUNT_INVALID";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type_snapshot", nullable = false, length = 20)
    private WorkspacePlanType planTypeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type_snapshot", nullable = false, length = 20)
    private SeatType seatTypeSnapshot;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "feature", nullable = false, length = 50)
    private String feature;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "credits_used", nullable = false, precision = 12, scale = 4)
    private BigDecimal creditsUsed;

    @Column(name = "billable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal billableAmount;

    @Column(name = "billing_status", nullable = false, length = 20)
    private String billingStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    private AiUsageEvent(
            UUID orgId,
            UUID userId,
            WorkspacePlanType planTypeSnapshot,
            SeatType seatTypeSnapshot,
            String category,
            String feature,
            String model,
            int inputTokens,
            int outputTokens,
            BigDecimal creditsUsed,
            BigDecimal billableAmount
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.userId = requireUserId(userId);
        this.planTypeSnapshot = requirePlanTypeSnapshot(planTypeSnapshot);
        this.seatTypeSnapshot = requireSeatTypeSnapshot(seatTypeSnapshot);
        this.category = requireCategory(category);
        this.feature = requireFeature(feature);
        this.model = requireModel(model);
        this.inputTokens = requireNonNegative(inputTokens, CODE_AI_USAGE_INPUT_TOKENS_INVALID, "입력 토큰은 0 이상이어야 합니다");
        this.outputTokens = requireNonNegative(outputTokens, CODE_AI_USAGE_OUTPUT_TOKENS_INVALID, "출력 토큰은 0 이상이어야 합니다");
        this.creditsUsed = requireCreditsUsed(creditsUsed);
        this.billableAmount = requireBillableAmount(billableAmount);
        this.billingStatus = BILLING_STATUS_PENDING;
        this.metadata = Map.of();
    }

    public static AiUsageEvent create(
            UUID orgId,
            UUID userId,
            WorkspacePlanType planTypeSnapshot,
            SeatType seatTypeSnapshot,
            String category,
            String feature,
            String model,
            int inputTokens,
            int outputTokens,
            BigDecimal creditsUsed,
            BigDecimal billableAmount
    ) {
        return new AiUsageEvent(
                orgId,
                userId,
                planTypeSnapshot,
                seatTypeSnapshot,
                category,
                feature,
                model,
                inputTokens,
                outputTokens,
                creditsUsed,
                billableAmount
        );
    }

    public void markBilled(BigDecimal billableAmount) {
        this.billableAmount = requireBillableAmount(billableAmount);
        this.billingStatus = BILLING_STATUS_BILLED;
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_ORGANIZATION_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }

    private WorkspacePlanType requirePlanTypeSnapshot(WorkspacePlanType value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_PLAN_TYPE_REQUIRED, "플랜 타입 스냅샷은 필수입니다");
        }
        return value;
    }

    private SeatType requireSeatTypeSnapshot(SeatType value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_SEAT_TYPE_REQUIRED, "좌석 타입 스냅샷은 필수입니다");
        }
        return value;
    }

    private String requireCategory(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_AI_USAGE_CATEGORY_REQUIRED, "카테고리는 필수입니다");
        }
        return value.trim();
    }

    private String requireFeature(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_AI_USAGE_FEATURE_REQUIRED, "기능명은 필수입니다");
        }
        return value.trim();
    }

    private String requireModel(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_AI_USAGE_MODEL_REQUIRED, "모델명은 필수입니다");
        }
        return value.trim();
    }

    private BigDecimal requireCreditsUsed(BigDecimal value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_CREDITS_REQUIRED, "사용 크레딧은 필수입니다");
        }
        if (value.signum() < 0) {
            throw new DomainException(CODE_AI_USAGE_CREDITS_INVALID, "사용 크레딧은 0 이상이어야 합니다");
        }
        return value;
    }

    private BigDecimal requireBillableAmount(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new DomainException(CODE_AI_USAGE_BILLABLE_AMOUNT_INVALID, "과금 금액은 0 이상이어야 합니다");
        }
        return value;
    }

    private int requireNonNegative(int value, String code, String message) {
        if (value < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }
}
