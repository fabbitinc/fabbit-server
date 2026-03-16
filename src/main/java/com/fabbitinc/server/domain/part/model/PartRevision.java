package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
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
                @Index(name = "ix_part_revisions_base_revision_id", columnList = "base_revision_id"),
                @Index(name = "ix_part_revisions_created_by", columnList = "created_by")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartRevision extends AbstractActorAuditableEntity implements AggregateRoot {

    public static final String CODE_PART_REVISION_PART_REQUIRED = "PART_REVISION_PART_REQUIRED";
    public static final String CODE_PART_REVISION_PART_NUMBER_REQUIRED = "PART_REVISION_PART_NUMBER_REQUIRED";
    public static final String CODE_PART_REVISION_PART_NUMBER_TOO_LONG = "PART_REVISION_PART_NUMBER_TOO_LONG";
    public static final String CODE_PART_REVISION_PART_NUMBER_INVALID_FORMAT = "PART_REVISION_PART_NUMBER_INVALID_FORMAT";
    public static final String CODE_PART_REVISION_CODE_REQUIRED = "PART_REVISION_CODE_REQUIRED";
    public static final String CODE_PART_REVISION_CODE_TOO_LONG = "PART_REVISION_CODE_TOO_LONG";
    public static final String CODE_PART_REVISION_CODE_INVALID_FORMAT = "PART_REVISION_CODE_INVALID_FORMAT";
    public static final String CODE_PART_REVISION_DRAFT_KEY_REQUIRED = "PART_REVISION_DRAFT_KEY_REQUIRED";
    public static final String CODE_PART_REVISION_DRAFT_KEY_TOO_LONG = "PART_REVISION_DRAFT_KEY_TOO_LONG";
    public static final String CODE_PART_REVISION_DRAFT_KEY_INVALID_FORMAT = "PART_REVISION_DRAFT_KEY_INVALID_FORMAT";
    public static final String CODE_PART_REVISION_NAME_TOO_LONG = "PART_REVISION_NAME_TOO_LONG";
    public static final String CODE_PART_REVISION_CATEGORY_TOO_LONG = "PART_REVISION_CATEGORY_TOO_LONG";
    public static final String CODE_PART_REVISION_MATERIAL_TOO_LONG = "PART_REVISION_MATERIAL_TOO_LONG";
    public static final String CODE_PART_REVISION_UNIT_TOO_LONG = "PART_REVISION_UNIT_TOO_LONG";
    public static final String CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID = "PART_REVISION_LEAD_TIME_DAYS_INVALID";
    public static final String CODE_PART_REVISION_STATUS_REQUIRED = "PART_REVISION_STATUS_REQUIRED";
    public static final String CODE_PART_REVISION_DRAFT_REQUIRED = "PART_REVISION_DRAFT_REQUIRED";
    public static final String CODE_PART_REVISION_DRAFT_SOURCE_REQUIRED = "PART_REVISION_DRAFT_SOURCE_REQUIRED";
    public static final String CODE_PART_REVISION_DRAFT_CODE_FORBIDDEN = "PART_REVISION_DRAFT_CODE_FORBIDDEN";
    public static final String CODE_PART_REVISION_APPROVABLE_REQUIRED = "PART_REVISION_APPROVABLE_REQUIRED";
    public static final String CODE_PART_REVISION_RELEASABLE_REQUIRED = "PART_REVISION_RELEASABLE_REQUIRED";
    public static final String CODE_PART_REVISION_SUPERSEDE_INVALID_STATE = "PART_REVISION_SUPERSEDE_INVALID_STATE";
    public static final String CODE_PART_REVISION_OWNER_REQUIRED = "PART_REVISION_OWNER_REQUIRED";
    public static final String CODE_PART_REVISION_OWNER_TEAM_REQUIRED = "PART_REVISION_OWNER_TEAM_REQUIRED";
    public static final String CODE_PART_REVISION_ENGINEERING_CHANGE_REQUIRED =
            "PART_REVISION_ENGINEERING_CHANGE_REQUIRED";
    public static final String CODE_PART_REVISION_ENGINEERING_CHANGE_INVALID_STATE =
            "PART_REVISION_ENGINEERING_CHANGE_INVALID_STATE";
    public static final String CODE_PART_REVISION_IN_REVIEW_REQUIRED = "PART_REVISION_IN_REVIEW_REQUIRED";

    private static final int MAX_REVISION_CODE_LENGTH = 50;
    private static final int MAX_DRAFT_KEY_LENGTH = 50;
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

    @Column(name = "revision_code", length = 50)
    private String revisionCode;

    @Column(name = "draft_key", length = 50)
    private String draftKey;

    @Column(name = "change_request_id")
    private UUID engineeringChangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PartRevisionStatus status;

    @Column(name = "name", length = 500)
    private String name;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User _createdByRelation;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    private User _updatedByRelation;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User _ownerRelation;

    @Column(name = "owner_team_id")
    private UUID ownerTeamId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_team_id", insertable = false, updatable = false)
    private Team _ownerTeamRelation;

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
            String draftKey,
            UUID baseRevisionId,
            String name,
            PartRevisionStatus status
    ) {
        super(UuidV7Generator.next());
        Part requiredPart = requirePart(part);
        this.partId = requiredPart.getId();
        this.partNumber = normalizePartNumber(requiredPart.getPartNumber());
        this._partRelation = requiredPart;
        this.status = requireStatus(status);
        this.revisionCode = normalizeRevisionCode(revisionCode, this.status);
        this.draftKey = normalizeDraftKey(draftKey, this.status);
        this.baseRevisionId = baseRevisionId;
        this.name = normalizeName(name);
        this.extendedProperties = "{}";
    }

    public static PartRevision createInitialDraft(Part part, String draftKey, String name, UUID actorId) {
        return initializeActor(
                new PartRevision(part, null, draftKey, null, name, PartRevisionStatus.DRAFT),
                actorId
        );
    }

    public static PartRevision createDraft(Part part, String draftKey, UUID baseRevisionId, String name, UUID actorId) {
        return initializeActor(
                new PartRevision(part, null, draftKey, baseRevisionId, name, PartRevisionStatus.DRAFT),
                actorId
        );
    }

    public static PartRevision createOfficial(
            Part part,
            String revisionCode,
            UUID baseRevisionId,
            String name,
            PartRevisionStatus status,
            UUID actorId
    ) {
        return initializeActor(
                new PartRevision(part, revisionCode, null, baseRevisionId, name, status),
                actorId
        );
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
        this.revisionCode = normalizeRevisionCode(revisionCode, this.status);
    }

    public void changeDraftKey(String draftKey) {
        this.draftKey = normalizeDraftKey(draftKey, this.status);
    }

    public void assignEngineeringChange(UUID engineeringChangeId) {
        if (engineeringChangeId == null) {
            throw new DomainException(
                    CODE_PART_REVISION_ENGINEERING_CHANGE_REQUIRED,
                    "변경관리 ID는 필수입니다"
            );
        }
        if (this.status != PartRevisionStatus.DRAFT && this.status != PartRevisionStatus.IN_REVIEW) {
            throw new DomainException(
                    CODE_PART_REVISION_ENGINEERING_CHANGE_INVALID_STATE,
                    "DRAFT 또는 IN_REVIEW 상태의 리비전만 변경관리에 연결할 수 있습니다"
            );
        }
        this.engineeringChangeId = engineeringChangeId;
    }

    public void clearEngineeringChange() {
        this.engineeringChangeId = null;
    }

    public void changePartNumber(String partNumber) {
        this.partNumber = normalizePartNumber(partNumber);
    }

    public void changeStatus(PartRevisionStatus status) {
        PartRevisionStatus nextStatus = requireStatus(status);
        this.status = nextStatus;
        this.revisionCode = normalizeRevisionCode(this.revisionCode, nextStatus);
        this.draftKey = normalizeDraftKey(this.draftKey, nextStatus);
    }

    public void changeName(String name) {
        this.name = normalizeName(name);
    }

    public void assignOwner(UUID ownerId) {
        if (ownerId == null) {
            throw new DomainException(CODE_PART_REVISION_OWNER_REQUIRED, "담당자 ID는 필수입니다");
        }
        this.ownerId = ownerId;
        if (this._ownerRelation != null && !ownerId.equals(this._ownerRelation.getId())) {
            this._ownerRelation = null;
        }
    }

    public void unassignOwner() {
        this.ownerId = null;
        this._ownerRelation = null;
    }

    public void assignOwnerTeam(UUID ownerTeamId) {
        if (ownerTeamId == null) {
            throw new DomainException(CODE_PART_REVISION_OWNER_TEAM_REQUIRED, "담당 팀 ID는 필수입니다");
        }
        this.ownerTeamId = ownerTeamId;
        if (this._ownerTeamRelation != null && !ownerTeamId.equals(this._ownerTeamRelation.getId())) {
            this._ownerTeamRelation = null;
        }
    }

    public void unassignOwnerTeam() {
        this.ownerTeamId = null;
        this._ownerTeamRelation = null;
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

    public void editDraft(PartRevisionDraftChanges changes, UUID actorId) {
        if (changes == null) {
            throw new DomainException(CODE_PART_REVISION_DRAFT_SOURCE_REQUIRED, "변경 내용은 필수입니다");
        }
        assertDraftEditable();
        mutate(actorId, () -> {
            if (changes.nameSet()) {
                this.name = normalizeName(changes.name());
            }
            if (changes.materialSet()) {
                this.material = normalizeMaterial(changes.material());
            }
            if (changes.unitSet()) {
                this.unit = normalizeUnit(changes.unit());
            }
            if (changes.descriptionSet()) {
                this.description = normalizeNullableText(changes.description());
            }
            if (changes.categorySet()) {
                this.category = normalizeCategory(changes.category());
            }
            if (changes.phantomSet()) {
                if (changes.phantom() == null) {
                    this.phantom = null;
                } else {
                    this.phantom = changes.phantom();
                }
            }
            if (changes.leadTimeDaysSet()) {
                if (changes.leadTimeDays() == null) {
                    this.leadTimeDays = null;
                } else {
                    if (changes.leadTimeDays() < 0) {
                        throw new DomainException(
                                CODE_PART_REVISION_LEAD_TIME_DAYS_INVALID,
                                "리드타임은 0 이상이어야 합니다"
                        );
                    }
                    this.leadTimeDays = changes.leadTimeDays();
                }
            }
            if (changes.extendedPropertiesSet()) {
                this.extendedProperties = normalizeExtendedProperties(changes.extendedProperties());
            }
        });
    }

    public void assertDraftEditable() {
        if (this.status != PartRevisionStatus.DRAFT) {
            throw new DomainException(CODE_PART_REVISION_DRAFT_REQUIRED, "DRAFT 상태의 리비전만 수정할 수 있습니다");
        }
    }

    public void assertDraftCreationAllowed() {
        if (this.status == PartRevisionStatus.DRAFT) {
            throw new DomainException(CODE_PART_REVISION_DRAFT_SOURCE_REQUIRED, "초안 리비전에서는 새 초안을 생성할 수 없습니다");
        }
    }

    public void approve(String revisionCode, UUID actorId) {
        mutate(actorId, () -> {
            assertApprovable();
            this.status = PartRevisionStatus.APPROVED;
            this.revisionCode = normalizeRevisionCode(revisionCode, this.status);
            this.draftKey = null;
        });
    }

    public void release(String revisionCode, UUID actorId) {
        mutate(actorId, () -> {
            if (this.status == PartRevisionStatus.APPROVED) {
                this.status = PartRevisionStatus.RELEASED;
                this.revisionCode = normalizeRevisionCode(revisionCode, this.status);
                this.draftKey = null;
                return;
            }

            assertReleasableDraft();
            this.status = PartRevisionStatus.RELEASED;
            this.revisionCode = normalizeRevisionCode(revisionCode, this.status);
            this.draftKey = null;
        });
    }

    public void markInReview(UUID actorId) {
        mutate(actorId, () -> {
            if (this.status == PartRevisionStatus.IN_REVIEW) {
                return;
            }
            if (this.status != PartRevisionStatus.DRAFT) {
                throw new DomainException(
                        CODE_PART_REVISION_ENGINEERING_CHANGE_INVALID_STATE,
                        "DRAFT 상태의 리비전만 IN_REVIEW로 전환할 수 있습니다"
                );
            }
            this.status = PartRevisionStatus.IN_REVIEW;
        });
    }

    public void revertToDraft(UUID actorId) {
        mutate(actorId, () -> {
            if (this.status == PartRevisionStatus.DRAFT) {
                return;
            }
            if (this.status != PartRevisionStatus.IN_REVIEW) {
                throw new DomainException(
                        CODE_PART_REVISION_IN_REVIEW_REQUIRED,
                        "IN_REVIEW 상태의 리비전만 DRAFT로 되돌릴 수 있습니다"
                );
            }
            this.status = PartRevisionStatus.DRAFT;
        });
    }

    public void markSuperseded(UUID actorId) {
        mutate(actorId, () -> {
            if (this.status == PartRevisionStatus.SUPERSEDED) {
                return;
            }
            if (this.status != PartRevisionStatus.APPROVED && this.status != PartRevisionStatus.RELEASED) {
                throw new DomainException(
                        CODE_PART_REVISION_SUPERSEDE_INVALID_STATE,
                        "공식 리비전만 SUPERSEDED 상태로 전환할 수 있습니다"
                );
            }
            this.status = PartRevisionStatus.SUPERSEDED;
            this.revisionCode = normalizeRevisionCode(this.revisionCode, this.status);
            this.draftKey = null;
        });
    }

    public void copyEditableFieldsFrom(PartRevision source) {
        if (source == null) {
            throw new DomainException(CODE_PART_REVISION_DRAFT_SOURCE_REQUIRED, "복제할 원본 리비전은 필수입니다");
        }
        changeName(source.getName());
        this.ownerId = source.getOwnerId();
        this.ownerTeamId = source.getOwnerTeamId();
        this._ownerRelation = null;
        this._ownerTeamRelation = null;
        changeCategory(source.getCategory());
        changeMaterial(source.getMaterial());
        changeUnit(source.getUnit());
        changeDescription(source.getDescription());
        this.phantom = source.getPhantom();
        this.leadTimeDays = source.getLeadTimeDays();
        this.extendedProperties = source.getExtendedProperties();
    }

    public PartRevisionActivity recordActivity(
            UUID actorId,
            PartRevisionActivityActionType actionType,
            PartRevisionActivitySourceType sourceType,
            UUID sourceRefId,
            String payload
    ) {
        touch(actorId);
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

    private void assertApprovable() {
        if (this.status != PartRevisionStatus.DRAFT && this.status != PartRevisionStatus.IN_REVIEW) {
            throw new DomainException(
                    CODE_PART_REVISION_APPROVABLE_REQUIRED,
                    "DRAFT 또는 IN_REVIEW 상태의 리비전만 승인할 수 있습니다"
            );
        }
    }

    private void assertReleasableDraft() {
        if (this.status != PartRevisionStatus.DRAFT && this.status != PartRevisionStatus.IN_REVIEW) {
            throw new DomainException(
                    CODE_PART_REVISION_RELEASABLE_REQUIRED,
                    "DRAFT 또는 IN_REVIEW 상태의 리비전만 바로 릴리즈할 수 있습니다"
            );
        }
    }

    private Part requirePart(Part value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_PART_REQUIRED, "파트는 필수입니다");
        }
        return value;
    }

    private String normalizeRevisionCode(String rawRevisionCode, PartRevisionStatus status) {
        if (status == PartRevisionStatus.DRAFT || status == PartRevisionStatus.IN_REVIEW) {
            if (rawRevisionCode == null || rawRevisionCode.isBlank()) {
                return null;
            }
            throw new DomainException(CODE_PART_REVISION_DRAFT_CODE_FORBIDDEN, "초안 상태에서는 공식 리비전 코드를 가질 수 없습니다");
        }

        if (rawRevisionCode == null || rawRevisionCode.isBlank()) {
            throw new DomainException(CODE_PART_REVISION_CODE_REQUIRED, "공식 리비전 코드는 필수입니다");
        }
        String trimmed = rawRevisionCode.trim();
        if (trimmed.length() > MAX_REVISION_CODE_LENGTH) {
            throw new DomainException(CODE_PART_REVISION_CODE_TOO_LONG, "리비전 코드는 50자 이하여야 합니다");
        }
        return PartRouteSegmentPolicy.validateRevisionCode(trimmed, CODE_PART_REVISION_CODE_INVALID_FORMAT);
    }

    private String normalizeDraftKey(String rawDraftKey, PartRevisionStatus status) {
        if (status == PartRevisionStatus.DRAFT || status == PartRevisionStatus.IN_REVIEW) {
            if (rawDraftKey == null || rawDraftKey.isBlank()) {
                throw new DomainException(CODE_PART_REVISION_DRAFT_KEY_REQUIRED, "초안 키는 필수입니다");
            }
        } else {
            return null;
        }

        String trimmed = rawDraftKey.trim();
        if (trimmed.length() > MAX_DRAFT_KEY_LENGTH) {
            throw new DomainException(CODE_PART_REVISION_DRAFT_KEY_TOO_LONG, "초안 키는 50자 이하여야 합니다");
        }
        return PartRouteSegmentPolicy.validateDraftKey(trimmed, CODE_PART_REVISION_DRAFT_KEY_INVALID_FORMAT);
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

    private static PartRevision initializeActor(PartRevision revision, UUID actorId) {
        revision.initializeActor(actorId);
        return revision;
    }

    @Override
    protected void afterActorTouched(UUID actorId, boolean createdByInitialized) {
        if (actorId != null) {
            if (createdByInitialized) {
                this._createdByRelation = null;
            }
            this._updatedByRelation = null;
        }
    }
}
