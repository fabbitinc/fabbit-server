package com.fabbitinc.server.domain.bom.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.part.model.PartRevision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "engineering_bom_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_engineering_bom_items_parent_revision_line_number",
                        columnNames = {"parent_part_revision_id", "line_number"}
                )
        },
        indexes = {
                @Index(name = "ix_engineering_bom_items_parent_part_revision_id", columnList = "parent_part_revision_id"),
                @Index(name = "ix_engineering_bom_items_child_part_revision_id", columnList = "child_part_revision_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringBomItem extends AbstractCreatedEntity {

    public static final String CODE_ENGINEERING_BOM_PARENT_REQUIRED = "ENGINEERING_BOM_PARENT_REQUIRED";
    public static final String CODE_ENGINEERING_BOM_LINE_NUMBER_REQUIRED = "ENGINEERING_BOM_LINE_NUMBER_REQUIRED";
    public static final String CODE_ENGINEERING_BOM_LINE_NUMBER_TOO_LONG = "ENGINEERING_BOM_LINE_NUMBER_TOO_LONG";
    public static final String CODE_ENGINEERING_BOM_CHILD_REQUIRED = "ENGINEERING_BOM_CHILD_REQUIRED";
    public static final String CODE_ENGINEERING_BOM_INVALID_QUANTITY = "ENGINEERING_BOM_INVALID_QUANTITY";
    public static final String CODE_ENGINEERING_BOM_SELF_LINK_NOT_ALLOWED = "ENGINEERING_BOM_SELF_LINK_NOT_ALLOWED";

    private static final int MAX_LINE_NUMBER_LENGTH = 50;

    @Column(name = "parent_part_revision_id", nullable = false)
    private UUID parentPartRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_part_revision_id", insertable = false, updatable = false)
    private PartRevision _parentPartRevisionRelation;

    @Column(name = "line_number", nullable = false, length = 50)
    private String lineNumber;

    @Column(name = "child_part_revision_id", nullable = false)
    private UUID childPartRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_part_revision_id", insertable = false, updatable = false)
    private PartRevision _childPartRevisionRelation;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    private EngineeringBomItem(
            UUID parentPartRevisionId,
            String lineNumber,
            UUID childPartRevisionId,
            BigDecimal quantity,
            String extendedProperties
    ) {
        super(UuidV7Generator.next());
        this.parentPartRevisionId = requireParentPartRevisionId(parentPartRevisionId);
        this.lineNumber = requireLineNumber(lineNumber);
        this.childPartRevisionId = requireChildPartRevisionId(childPartRevisionId);
        requireDifferentRevisions(this.parentPartRevisionId, this.childPartRevisionId);
        this.quantity = requireQuantity(quantity);
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    public static EngineeringBomItem add(
            UUID parentPartRevisionId,
            String lineNumber,
            UUID childPartRevisionId,
            BigDecimal quantity,
            String extendedProperties
    ) {
        return new EngineeringBomItem(
                parentPartRevisionId,
                lineNumber,
                childPartRevisionId,
                quantity,
                extendedProperties
        );
    }

    public void changeChildPartRevision(UUID childPartRevisionId) {
        UUID nextChildPartRevisionId = requireChildPartRevisionId(childPartRevisionId);
        requireDifferentRevisions(this.parentPartRevisionId, nextChildPartRevisionId);
        this.childPartRevisionId = nextChildPartRevisionId;
        if (this._childPartRevisionRelation != null && !nextChildPartRevisionId.equals(this._childPartRevisionRelation.getId())) {
            this._childPartRevisionRelation = null;
        }
    }

    public void changeLineNumber(String lineNumber) {
        this.lineNumber = requireLineNumber(lineNumber);
    }

    public void changeQuantity(BigDecimal quantity) {
        this.quantity = requireQuantity(quantity);
    }

    public void changeExtendedProperties(String extendedProperties) {
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    private UUID requireParentPartRevisionId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_BOM_PARENT_REQUIRED, "상위 부품 리비전 ID는 필수입니다");
        }
        return value;
    }

    private String requireLineNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ENGINEERING_BOM_LINE_NUMBER_REQUIRED, "BOM 줄 번호는 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LINE_NUMBER_LENGTH) {
            throw new DomainException(CODE_ENGINEERING_BOM_LINE_NUMBER_TOO_LONG, "BOM 줄 번호는 50자 이하여야 합니다");
        }
        return trimmed;
    }

    private UUID requireChildPartRevisionId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_BOM_CHILD_REQUIRED, "하위 부품 리비전 ID는 필수입니다");
        }
        return value;
    }

    private BigDecimal requireQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException(CODE_ENGINEERING_BOM_INVALID_QUANTITY, "수량은 0보다 커야 합니다");
        }
        return value.stripTrailingZeros();
    }

    private void requireDifferentRevisions(UUID parentRevisionId, UUID childRevisionId) {
        if (parentRevisionId.equals(childRevisionId)) {
            throw new DomainException(
                    CODE_ENGINEERING_BOM_SELF_LINK_NOT_ALLOWED,
                    "상위 부품 리비전과 하위 부품 리비전은 같을 수 없습니다"
            );
        }
    }

    private String normalizeExtendedProperties(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw.trim();
    }
}
