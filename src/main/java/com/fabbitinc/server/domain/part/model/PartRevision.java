package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "part_revisions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_revisions_part_number_revision_code",
                        columnNames = {"part_number", "revision_code"}
                )
        },
        indexes = {
                @Index(name = "ix_part_revisions_part_id", columnList = "part_id"),
                @Index(name = "ix_part_revisions_part_number", columnList = "part_number"),
                @Index(name = "ix_part_revisions_base_revision_id", columnList = "base_revision_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartRevision extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_PART_REVISION_PART_REQUIRED = "PART_REVISION_PART_REQUIRED";
    public static final String CODE_PART_REVISION_PART_NUMBER_REQUIRED = "PART_REVISION_PART_NUMBER_REQUIRED";
    public static final String CODE_PART_REVISION_PART_NUMBER_TOO_LONG = "PART_REVISION_PART_NUMBER_TOO_LONG";
    public static final String CODE_PART_REVISION_PART_NUMBER_INVALID_FORMAT = "PART_REVISION_PART_NUMBER_INVALID_FORMAT";
    public static final String CODE_PART_REVISION_CODE_REQUIRED = "PART_REVISION_CODE_REQUIRED";
    public static final String CODE_PART_REVISION_CODE_TOO_LONG = "PART_REVISION_CODE_TOO_LONG";
    public static final String CODE_PART_REVISION_CODE_INVALID_FORMAT = "PART_REVISION_CODE_INVALID_FORMAT";
    public static final String CODE_PART_REVISION_NAME_TOO_LONG = "PART_REVISION_NAME_TOO_LONG";
    public static final String CODE_PART_REVISION_CATEGORY_TOO_LONG = "PART_REVISION_CATEGORY_TOO_LONG";
    public static final String CODE_PART_REVISION_MATERIAL_TOO_LONG = "PART_REVISION_MATERIAL_TOO_LONG";
    public static final String CODE_PART_REVISION_UNIT_TOO_LONG = "PART_REVISION_UNIT_TOO_LONG";
    public static final String CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID = "PART_REVISION_LEAD_TIME_DAYS_INVALID";
    public static final String CODE_PART_REVISION_STATUS_REQUIRED = "PART_REVISION_STATUS_REQUIRED";

    private static final int MAX_REVISION_CODE_LENGTH = 50;
    private static final int MAX_PART_NUMBER_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 500;
    private static final int MAX_CATEGORY_LENGTH = 100;
    private static final int MAX_MATERIAL_LENGTH = 200;
    private static final int MAX_UNIT_LENGTH = 20;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", insertable = false, updatable = false)
    private Part _partRelation;

    @Column(name = "base_revision_id")
    private UUID baseRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_revision_id", insertable = false, updatable = false)
    private PartRevision _baseRevisionRelation;

    @Column(name = "revision_code", nullable = false, length = 50)
    private String revisionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PartRevisionStatus status;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "material", length = 200)
    private String material;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "is_phantom")
    private Boolean phantom;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties = "{}";

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "partRevision", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartRevisionActivity> activities = new ArrayList<>();

    private PartRevision(
            Part part,
            String revisionCode,
            UUID baseRevisionId,
            String name,
            PartRevisionStatus status
    ) {
        super(UuidV7Generator.next());
        Part requiredPart = requirePart(part);
        this.partId = requiredPart.getId();
        this.partNumber = normalizePartNumber(requiredPart.getPartNumber());
        this._partRelation = requiredPart;
        this.revisionCode = normalizeRevisionCode(revisionCode);
        this.baseRevisionId = baseRevisionId;
        this.name = normalizeName(name);
        this.status = requireStatus(status);
        this.extendedProperties = "{}";
    }

    public static PartRevision createInitial(Part part, String revisionCode, String name) {
        return new PartRevision(part, revisionCode, null, name, PartRevisionStatus.DRAFT);
    }

    public static PartRevision createDraft(Part part, String revisionCode, UUID baseRevisionId, String name) {
        return new PartRevision(part, revisionCode, baseRevisionId, name, PartRevisionStatus.DRAFT);
    }

    public void assignBaseRevision(UUID baseRevisionId) {
        this.baseRevisionId = baseRevisionId;
        if (this._baseRevisionRelation != null && (baseRevisionId == null || !baseRevisionId.equals(this._baseRevisionRelation.getId()))) {
            this._baseRevisionRelation = null;
        }
    }

    public void clearBaseRevision() {
        this.baseRevisionId = null;
        this._baseRevisionRelation = null;
    }

    public void changeRevisionCode(String revisionCode) {
        this.revisionCode = normalizeRevisionCode(revisionCode);
    }

    public void changePartNumber(String partNumber) {
        this.partNumber = normalizePartNumber(partNumber);
    }

    public void changeStatus(PartRevisionStatus status) {
        this.status = requireStatus(status);
    }

    public void changeName(String name) {
        this.name = normalizeName(name);
    }

    public void changeCategory(String category) {
        this.category = normalizeCategory(category);
    }

    public void changeMaterial(String material) {
        this.material = normalizeMaterial(material);
    }

    public void changeUnit(String unit) {
        this.unit = normalizeUnit(unit);
    }

    public void changeDescription(String description) {
        this.description = normalizeNullableText(description);
    }

    public void markPhantom() {
        this.phantom = true;
    }

    public void markReal() {
        this.phantom = false;
    }

    public void clearPhantomFlag() {
        this.phantom = null;
    }

    public void changeLeadTimeDays(Integer leadTimeDays) {
        if (leadTimeDays == null) {
            this.leadTimeDays = null;
            return;
        }
        if (leadTimeDays < 0) {
            throw new DomainException(CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID, "리드타임은 0 이상이어야 합니다");
        }
        this.leadTimeDays = leadTimeDays;
    }

    public void changeExtendedProperties(String extendedProperties) {
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    public PartRevisionActivity recordActivity(
            UUID actorId,
            PartRevisionActivityActionType actionType,
            PartRevisionActivitySourceType sourceType,
            UUID sourceRefId,
            String payload
    ) {
        return appendActivity(PartRevisionActivity.record(this, actorId, actionType, sourceType, sourceRefId, payload));
    }

    public PartRevisionActivity recordActivityAt(
            UUID actorId,
            PartRevisionActivityActionType actionType,
            PartRevisionActivitySourceType sourceType,
            UUID sourceRefId,
            String payload,
            Instant occurredAt
    ) {
        return appendActivity(PartRevisionActivity.recordAt(this, actorId, actionType, sourceType, sourceRefId, payload, occurredAt));
    }

    public List<PartRevisionActivity> getActivities() {
        return List.copyOf(activities);
    }

    private Part requirePart(Part value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_PART_REQUIRED, "파트는 필수입니다");
        }
        return value;
    }

    private String normalizeRevisionCode(String rawRevisionCode) {
        if (rawRevisionCode == null || rawRevisionCode.isBlank()) {
            throw new DomainException(CODE_PART_REVISION_CODE_REQUIRED, "리비전 코드는 필수입니다");
        }
        String trimmed = rawRevisionCode.trim();
        if (trimmed.length() > MAX_REVISION_CODE_LENGTH) {
            throw new DomainException(CODE_PART_REVISION_CODE_TOO_LONG, "리비전 코드는 50자 이하여야 합니다");
        }
        return PartRouteSegmentPolicy.validateRevisionCode(trimmed, CODE_PART_REVISION_CODE_INVALID_FORMAT);
    }

    private String normalizePartNumber(String rawPartNumber) {
        if (rawPartNumber == null || rawPartNumber.isBlank()) {
            throw new DomainException(CODE_PART_REVISION_PART_NUMBER_REQUIRED, "품번은 필수입니다");
        }
        String trimmed = rawPartNumber.trim();
        if (trimmed.length() > MAX_PART_NUMBER_LENGTH) {
            throw new DomainException(CODE_PART_REVISION_PART_NUMBER_TOO_LONG, "품번은 100자 이하여야 합니다");
        }
        return PartRouteSegmentPolicy.validatePartNumber(trimmed, CODE_PART_REVISION_PART_NUMBER_INVALID_FORMAT);
    }

    private PartRevisionStatus requireStatus(PartRevisionStatus value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_STATUS_REQUIRED, "리비전 상태는 필수입니다");
        }
        return value;
    }

    private String normalizeName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_PART_REVISION_NAME_TOO_LONG, "품명은 500자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizeCategory(String rawCategory) {
        if (rawCategory == null) {
            return null;
        }
        String trimmed = rawCategory.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > MAX_CATEGORY_LENGTH) {
            throw new DomainException(CODE_PART_REVISION_CATEGORY_TOO_LONG, "카테고리는 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizeMaterial(String rawMaterial) {
        return normalizeWithMaxLength(
                rawMaterial,
                MAX_MATERIAL_LENGTH,
                CODE_PART_REVISION_MATERIAL_TOO_LONG,
                "재질은 200자 이하여야 합니다"
        );
    }

    private String normalizeUnit(String rawUnit) {
        return normalizeWithMaxLength(
                rawUnit,
                MAX_UNIT_LENGTH,
                CODE_PART_REVISION_UNIT_TOO_LONG,
                "단위는 20자 이하여야 합니다"
        );
    }

    private String normalizeWithMaxLength(String raw, int maxLength, String code, String message) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new DomainException(code, message);
        }
        return trimmed;
    }

    private String normalizeNullableText(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeExtendedProperties(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw.trim();
    }

    private PartRevisionActivity appendActivity(PartRevisionActivity activity) {
        this.activities.add(activity);
        return activity;
    }
}
