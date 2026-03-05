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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "bom_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_bom_links_parent_part_id_child_part_id",
                        columnNames = {"parent_part_id", "child_part_id"}
                )
        },
        indexes = {
                @Index(name = "ix_bom_links_parent_part_id", columnList = "parent_part_id"),
                @Index(name = "ix_bom_links_child_part_id", columnList = "child_part_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BomLink extends AbstractCreatedEntity {

    @Column(name = "parent_part_id", nullable = false)
    private UUID parentPartId;

    @Column(name = "child_part_id", nullable = false)
    private UUID childPartId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    public BomLink(UUID parentPartId, UUID childPartId) {
        this(parentPartId, childPartId, 1, "{}");
    }

    public BomLink(UUID parentPartId, UUID childPartId, int quantity, String extendedProperties) {
        super(UuidV7Generator.next());
        this.parentPartId = parentPartId;
        this.childPartId = childPartId;
        this.quantity = quantity;
        this.extendedProperties = (extendedProperties == null || extendedProperties.isBlank())
                ? "{}"
                : extendedProperties;
    }
}
