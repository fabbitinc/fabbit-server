package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
        name = "parts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_parts_part_number", columnNames = "part_number")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Part extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_PART_NUMBER_REQUIRED = "PART_NUMBER_REQUIRED";
    public static final String CODE_PART_NUMBER_TOO_LONG = "PART_NUMBER_TOO_LONG";
    public static final String CODE_PART_NUMBER_INVALID_FORMAT = "PART_NUMBER_INVALID_FORMAT";
    public static final String CODE_PART_RELEASED_REVISION_REQUIRED = "PART_RELEASED_REVISION_REQUIRED";
    public static final String CODE_PART_LIFECYCLE_TRANSITION_INVALID = "PART_LIFECYCLE_TRANSITION_INVALID";
    public static final String CODE_PART_OBSOLETE = "PART_OBSOLETE";

    private static final int MAX_PART_NUMBER_LENGTH = 100;

    @Column(name = "current_released_revision_id")
    private UUID currentReleasedRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_released_revision_id", insertable = false, updatable = false)
    private PartRevision _currentReleasedRevisionRelation;

    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 50)
    private PartLifecycleState lifecycleState;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 30)
    private PartItemType itemType;

    @Column(name = "numbering_category_id")
    private UUID categoryId;

    private Part(String partNumber) {
        super(UuidV7Generator.next());
        this.partNumber = validatePartNumber(partNumber);
        this.lifecycleState = PartLifecycleState.ACTIVE;
    }

    private Part(String partNumber, UUID categoryId, PartItemType itemType) {
        super(UuidV7Generator.next());
        this.partNumber = validatePartNumber(partNumber);
        this.lifecycleState = PartLifecycleState.ACTIVE;
        this.categoryId = validateCategoryId(categoryId);
        this.itemType = validateItemType(itemType);
    }

    public static Part create(String partNumber) {
        return new Part(partNumber);
    }

    public static Part create(String partNumber, UUID categoryId, PartItemType itemType) {
        return new Part(partNumber, categoryId, itemType);
    }

    public void changeItemType(PartItemType itemType) {
        this.itemType = validateItemType(itemType);
    }

    public void changeCategoryId(UUID categoryId) {
        this.categoryId = validateCategoryId(categoryId);
    }

    public void changeLifecycleState(PartLifecycleState targetState) {
        if (!this.lifecycleState.canTransitionTo(targetState)) {
            throw new DomainException(
                    CODE_PART_LIFECYCLE_TRANSITION_INVALID,
                    "%s 상태에서 %s 상태로 전환할 수 없습니다".formatted(this.lifecycleState, targetState)
            );
        }
        this.lifecycleState = targetState;
    }

    public void forceLifecycleState(PartLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public void resetLifecycleState() {
        this.lifecycleState = PartLifecycleState.ACTIVE;
    }

    public void assertNotObsolete() {
        if (this.lifecycleState == PartLifecycleState.OBSOLETE) {
            throw new DomainException(CODE_PART_OBSOLETE, "폐기된 부품에는 새 초안을 생성할 수 없습니다");
        }
    }

    public void assignCurrentReleasedRevision(UUID revisionId) {
        if (revisionId == null) {
            throw new DomainException(CODE_PART_RELEASED_REVISION_REQUIRED, "릴리즈 리비전 ID는 필수입니다");
        }
        this.currentReleasedRevisionId = revisionId;
        if (this._currentReleasedRevisionRelation != null && !revisionId.equals(this._currentReleasedRevisionRelation.getId())) {
            this._currentReleasedRevisionRelation = null;
        }
    }

    public void clearCurrentReleasedRevision() {
        this.currentReleasedRevisionId = null;
        this._currentReleasedRevisionRelation = null;
    }

    private String validatePartNumber(String rawPartNumber) {
        if (rawPartNumber == null || rawPartNumber.isBlank()) {
            throw new DomainException(CODE_PART_NUMBER_REQUIRED, "품번은 필수입니다");
        }
        String trimmed = rawPartNumber.trim();
        if (trimmed.length() > MAX_PART_NUMBER_LENGTH) {
            throw new DomainException(CODE_PART_NUMBER_TOO_LONG, "품번은 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private UUID validateCategoryId(UUID categoryId) {
        if (categoryId == null) {
            throw new DomainException(CODE_PART_NUMBER_REQUIRED, "카테고리는 필수입니다");
        }
        return categoryId;
    }

    private PartItemType validateItemType(PartItemType itemType) {
        if (itemType == null) {
            throw new DomainException(CODE_PART_NUMBER_REQUIRED, "itemType은 필수입니다");
        }
        return itemType;
    }
}
