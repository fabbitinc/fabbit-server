package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "part_suppliers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_suppliers_part_id_supplier_id",
                        columnNames = {"part_id", "supplier_id"}
                )
        },
        indexes = {
                @Index(name = "ix_part_suppliers_part_id", columnList = "part_id"),
                @Index(name = "ix_part_suppliers_supplier_id", columnList = "supplier_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartSupplier extends AbstractCreatedEntity {

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    public PartSupplier(UUID partId, UUID supplierId, Double unitCost, String extendedProperties) {
        super(UuidV7Generator.next());
        this.partId = partId;
        this.supplierId = supplierId;
        this.unitCost = unitCost;
        this.extendedProperties = (extendedProperties == null || extendedProperties.isBlank())
                ? "{}"
                : extendedProperties;
    }
}
