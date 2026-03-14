package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.supplier.model.Supplier;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "part_suppliers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_suppliers_part_revision_id_supplier_id",
                        columnNames = {"part_revision_id", "supplier_id"}
                )
        },
        indexes = {
                @Index(name = "ix_part_suppliers_part_revision_id", columnList = "part_revision_id"),
                @Index(name = "ix_part_suppliers_supplier_id", columnList = "supplier_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartSupplier extends AbstractCreatedEntity {

    public static final String CODE_PART_SUPPLIER_PART_REVISION_REQUIRED = "PART_SUPPLIER_PART_REVISION_REQUIRED";
    public static final String CODE_PART_SUPPLIER_SUPPLIER_REQUIRED = "PART_SUPPLIER_SUPPLIER_REQUIRED";
    public static final String CODE_PART_SUPPLIER_UNIT_COST_INVALID = "PART_SUPPLIER_UNIT_COST_INVALID";

    @Column(name = "part_revision_id", nullable = false)
    private UUID partRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_revision_id", insertable = false, updatable = false)
    private PartRevision _partRevisionRelation;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private Supplier _supplierRelation;

    @Column(name = "unit_cost")
    private Double unitCost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    private PartSupplier(UUID partRevisionId, UUID supplierId, Double unitCost, String extendedProperties) {
        super(UuidV7Generator.next());
        this.partRevisionId = requirePartRevisionId(partRevisionId);
        this.supplierId = requireSupplierId(supplierId);
        this.unitCost = normalizeUnitCost(unitCost);
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    public static PartSupplier link(UUID partRevisionId, UUID supplierId, Double unitCost, String extendedProperties) {
        return new PartSupplier(partRevisionId, supplierId, unitCost, extendedProperties);
    }

    public void changeUnitCost(Double unitCost) {
        this.unitCost = normalizeUnitCost(unitCost);
    }

    public void changeExtendedProperties(String extendedProperties) {
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    private UUID requirePartRevisionId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_PART_SUPPLIER_PART_REVISION_REQUIRED, "부품 리비전 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireSupplierId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_PART_SUPPLIER_SUPPLIER_REQUIRED, "공급사 ID는 필수입니다");
        }
        return value;
    }

    private Double normalizeUnitCost(Double value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value.isNaN() || value.isInfinite()) {
            throw new DomainException(CODE_PART_SUPPLIER_UNIT_COST_INVALID, "단가는 0 이상의 유효한 숫자여야 합니다");
        }
        return value;
    }

    private String normalizeExtendedProperties(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw.trim();
    }
}
