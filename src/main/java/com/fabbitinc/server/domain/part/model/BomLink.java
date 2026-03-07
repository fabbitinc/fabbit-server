package com.fabbitinc.server.domain.part.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    public static final String CODE_BOM_PARENT_REQUIRED = "BOM_PARENT_REQUIRED";
    public static final String CODE_BOM_CHILD_REQUIRED = "BOM_CHILD_REQUIRED";
    public static final String CODE_BOM_INVALID_QUANTITY = "BOM_INVALID_QUANTITY";
    public static final String CODE_BOM_SELF_LINK_NOT_ALLOWED = "BOM_SELF_LINK_NOT_ALLOWED";

    @Column(name = "parent_part_id", nullable = false)
    private UUID parentPartId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_part_id", insertable = false, updatable = false)
    private Part _parentPartRelation;

    @Column(name = "child_part_id", nullable = false)
    private UUID childPartId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_part_id", insertable = false, updatable = false)
    private Part _childPartRelation;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    private BomLink(UUID parentPartId, UUID childPartId, int quantity, String extendedProperties) {
        super(UuidV7Generator.next());
        this.parentPartId = requireParentPartId(parentPartId);
        this.childPartId = requireChildPartId(childPartId);
        requireDifferentParts(this.parentPartId, this.childPartId);
        this.quantity = requireQuantity(quantity);
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    public static BomLink connect(UUID parentPartId, UUID childPartId, int quantity, String extendedProperties) {
        return new BomLink(parentPartId, childPartId, quantity, extendedProperties);
    }

    public void changeQuantity(int quantity) {
        this.quantity = requireQuantity(quantity);
    }

    public void changeExtendedProperties(String extendedProperties) {
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    private UUID requireParentPartId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_BOM_PARENT_REQUIRED, "상위 부품 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireChildPartId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_BOM_CHILD_REQUIRED, "하위 부품 ID는 필수입니다");
        }
        return value;
    }

    private int requireQuantity(int value) {
        if (value <= 0) {
            throw new DomainException(CODE_BOM_INVALID_QUANTITY, "수량은 1 이상이어야 합니다");
        }
        return value;
    }

    private void requireDifferentParts(UUID parentId, UUID childId) {
        if (parentId.equals(childId)) {
            throw new DomainException(CODE_BOM_SELF_LINK_NOT_ALLOWED, "상위 부품과 하위 부품은 같을 수 없습니다");
        }
    }

    private String normalizeExtendedProperties(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw.trim();
    }
}
