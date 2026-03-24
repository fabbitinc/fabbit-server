package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "engineering_change_affected_items",
        indexes = {
                @Index(name = "ix_ec_affected_items_ec_id", columnList = "engineering_change_id"),
                @Index(name = "ix_ec_affected_items_target_id", columnList = "target_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChangeAffectedItem extends AbstractCreatedEntity {

    public static final String CODE_EC_AFFECTED_ITEM_TYPE_REQUIRED = "EC_AFFECTED_ITEM_TYPE_REQUIRED";
    public static final String CODE_EC_AFFECTED_ITEM_TARGET_REQUIRED = "EC_AFFECTED_ITEM_TARGET_REQUIRED";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange _engineeringChangeRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private EngineeringChangeAffectedItemType itemType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "action_detail", columnDefinition = "text")
    private String actionDetail;

    private EngineeringChangeAffectedItem(
            UUID engineeringChangeId,
            EngineeringChangeAffectedItemType itemType,
            UUID targetId,
            String actionDetail
    ) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = engineeringChangeId;
        this.itemType = requireItemType(itemType);
        this.targetId = requireTargetId(targetId);
        this.actionDetail = actionDetail;
    }

    public static EngineeringChangeAffectedItem create(
            UUID engineeringChangeId,
            EngineeringChangeAffectedItemType itemType,
            UUID targetId,
            String actionDetail
    ) {
        return new EngineeringChangeAffectedItem(engineeringChangeId, itemType, targetId, actionDetail);
    }

    private EngineeringChangeAffectedItemType requireItemType(EngineeringChangeAffectedItemType value) {
        if (value == null) {
            throw new DomainException(CODE_EC_AFFECTED_ITEM_TYPE_REQUIRED, "영향 항목 유형은 필수입니다");
        }
        return value;
    }

    private UUID requireTargetId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_EC_AFFECTED_ITEM_TARGET_REQUIRED, "대상 ID는 필수입니다");
        }
        return value;
    }
}
