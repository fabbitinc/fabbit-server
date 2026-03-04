package com.fabbitinc.server.domain.aiusage.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

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

    public AiUsageLog(
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
        this.orgId = orgId;
        this.userId = userId;
        this.category = category;
        this.feature = feature;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.creditsUsed = creditsUsed;
    }
}
