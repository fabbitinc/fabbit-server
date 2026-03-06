package com.fabbitinc.server.domain.aiusage.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "ai_usage_logs",
        schema = "public",
        indexes = {
                @Index(name = "ix_ai_usage_logs_org_id", columnList = "org_id"),
                @Index(name = "ix_ai_usage_logs_user_id", columnList = "user_id"),
                @Index(name = "ix_ai_usage_logs_org_id_created_at", columnList = "org_id,created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsageLog extends AbstractCreatedEntity {

    public static final String CODE_AI_USAGE_ORG_REQUIRED = "AI_USAGE_ORG_REQUIRED";
    public static final String CODE_AI_USAGE_USER_REQUIRED = "AI_USAGE_USER_REQUIRED";
    public static final String CODE_AI_USAGE_CATEGORY_REQUIRED = "AI_USAGE_CATEGORY_REQUIRED";
    public static final String CODE_AI_USAGE_FEATURE_REQUIRED = "AI_USAGE_FEATURE_REQUIRED";
    public static final String CODE_AI_USAGE_MODEL_REQUIRED = "AI_USAGE_MODEL_REQUIRED";
    public static final String CODE_AI_USAGE_CREDITS_REQUIRED = "AI_USAGE_CREDITS_REQUIRED";
    public static final String CODE_AI_USAGE_INPUT_TOKENS_INVALID = "AI_USAGE_INPUT_TOKENS_INVALID";
    public static final String CODE_AI_USAGE_OUTPUT_TOKENS_INVALID = "AI_USAGE_OUTPUT_TOKENS_INVALID";
    public static final String CODE_AI_USAGE_CREDITS_INVALID = "AI_USAGE_CREDITS_INVALID";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "org_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_ai_usage_logs_org_id")
    )
    private Organization _organizationRelation;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_ai_usage_logs_user_id")
    )
    private User _userRelation;

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

    @Column(name = "credits_used", nullable = false, precision = 10, scale = 4)
    private BigDecimal creditsUsed;

    private AiUsageLog(
            UUID orgId,
            UUID userId,
            String category,
            String feature,
            String model,
            int inputTokens,
            int outputTokens,
            BigDecimal creditsUsed
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.userId = requireUserId(userId);
        this.category = requireCategory(category);
        this.feature = requireFeature(feature);
        this.model = requireModel(model);
        this.inputTokens = requireNonNegative(inputTokens, CODE_AI_USAGE_INPUT_TOKENS_INVALID, "입력 토큰은 0 이상이어야 합니다");
        this.outputTokens = requireNonNegative(outputTokens, CODE_AI_USAGE_OUTPUT_TOKENS_INVALID, "출력 토큰은 0 이상이어야 합니다");
        this.creditsUsed = requireCreditsUsed(creditsUsed);
    }

    public static AiUsageLog create(
            UUID orgId,
            UUID userId,
            String category,
            String feature,
            String model,
            int inputTokens,
            int outputTokens,
            BigDecimal creditsUsed
    ) {
        return new AiUsageLog(
                orgId,
                userId,
                category,
                feature,
                model,
                inputTokens,
                outputTokens,
                creditsUsed
        );
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_AI_USAGE_USER_REQUIRED, "사용자 ID는 필수입니다");
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

    private int requireNonNegative(int value, String code, String message) {
        if (value < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }
}
