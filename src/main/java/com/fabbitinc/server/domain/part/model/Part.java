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

    private Part(String partNumber) {
        super(UuidV7Generator.next());
        this.partNumber = validatePartNumber(partNumber);
        this.lifecycleState = PartLifecycleState.ACTIVE;
    }

    public static Part create(String partNumber) {
        return new Part(partNumber);
    }

    public void changeLifecycleState(PartLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public void resetLifecycleState() {
        this.lifecycleState = PartLifecycleState.ACTIVE;
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
        return PartRouteSegmentPolicy.validatePartNumber(trimmed, CODE_PART_NUMBER_INVALID_FORMAT);
    }
}
