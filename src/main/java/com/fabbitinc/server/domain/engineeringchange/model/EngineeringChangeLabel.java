package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "engineering_change_labels",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_engineering_change_labels_ec_id_label_id",
                        columnNames = {"engineering_change_id", "label_id"}
                )
        },
        indexes = {
                @Index(name = "ix_engineering_change_labels_ec_id", columnList = "engineering_change_id"),
                @Index(name = "ix_engineering_change_labels_label_id", columnList = "label_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChangeLabel extends AbstractCreatedEntity {

    public static final String CODE_ENGINEERING_CHANGE_LABEL_EC_REQUIRED = "ENGINEERING_CHANGE_LABEL_EC_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_LABEL_LABEL_REQUIRED = "ENGINEERING_CHANGE_LABEL_LABEL_REQUIRED";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange engineeringChange;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    private EngineeringChangeLabel(UUID engineeringChangeId, UUID labelId) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = requireEngineeringChangeId(engineeringChangeId);
        this.labelId = requireLabelId(labelId);
    }

    public static EngineeringChangeLabel link(EngineeringChange engineeringChange, UUID labelId) {
        if (engineeringChange == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_LABEL_EC_REQUIRED, "변경관리 ID는 필수입니다");
        }
        if (labelId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_LABEL_LABEL_REQUIRED, "라벨 ID는 필수입니다");
        }
        EngineeringChangeLabel link = new EngineeringChangeLabel(engineeringChange.getId(), labelId);
        link.engineeringChange = engineeringChange;
        return link;
    }

    private UUID requireEngineeringChangeId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_LABEL_EC_REQUIRED, "변경관리 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireLabelId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_LABEL_LABEL_REQUIRED, "라벨 ID는 필수입니다");
        }
        return value;
    }
}
