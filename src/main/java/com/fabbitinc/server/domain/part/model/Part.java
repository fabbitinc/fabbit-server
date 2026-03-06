package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;
import java.util.regex.Pattern;

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
    public static final String CODE_PART_NAME_TOO_LONG = "PART_NAME_TOO_LONG";
    public static final String CODE_PART_CATEGORY_TOO_LONG = "PART_CATEGORY_TOO_LONG";
    public static final String CODE_PART_MATERIAL_TOO_LONG = "PART_MATERIAL_TOO_LONG";
    public static final String CODE_PART_UNIT_TOO_LONG = "PART_UNIT_TOO_LONG";
    public static final String CODE_PART_DRAWING_REQUIRED = "PART_DRAWING_REQUIRED";
    public static final String CODE_PART_OWNER_REQUIRED = "PART_OWNER_REQUIRED";
    public static final String CODE_PART_OWNER_TEAM_REQUIRED = "PART_OWNER_TEAM_REQUIRED";
    public static final String CODE_PART_LEAD_TIME_DAYS_INVALID = "PART_LEAD_TIME_DAYS_INVALID";

    private static final int MAX_PART_NUMBER_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 500;
    private static final int MAX_CATEGORY_LENGTH = 100;
    private static final int MAX_MATERIAL_LENGTH = 200;
    private static final int MAX_UNIT_LENGTH = 20;
    private static final Pattern REVISION_SUFFIX_PATTERN = Pattern.compile("^(.*?)(\\d+|[A-Z])$", Pattern.CASE_INSENSITIVE);

    @Column(name = "drawing_id")
    private UUID drawingId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drawing_id", insertable = false, updatable = false)
    private Drawing _drawingRelation;

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

    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "revision", nullable = false, length = 50)
    private String revision = "1";

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

    @Convert(converter = PartLifecycleStateConverter.class)
    @Column(name = "lifecycle_state", length = 50)
    private PartLifecycleState lifecycleState;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties = "{}";

    private Part(String partNumber, String name) {
        super(UuidV7Generator.next());
        this.partNumber = validatePartNumber(partNumber);
        this.name = normalizeName(name);
        this.revision = "1";
        this.extendedProperties = "{}";
    }

    public static Part create(String partNumber, String name) {
        return new Part(partNumber, name);
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
        this.description = normalizeDescription(description);
    }

    public void changeExtendedProperties(String extendedProperties) {
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
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

    public void changeLifecycleState(PartLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public void clearLifecycleState() {
        this.lifecycleState = null;
    }

    public void changeLeadTimeDays(Integer leadTimeDays) {
        if (leadTimeDays == null) {
            this.leadTimeDays = null;
            return;
        }
        if (leadTimeDays < 0) {
            throw new DomainException(CODE_PART_LEAD_TIME_DAYS_INVALID, "리드타임은 0 이상이어야 합니다");
        }
        this.leadTimeDays = leadTimeDays;
    }

    public void assignOwner(UUID ownerId) {
        if (ownerId == null) {
            throw new DomainException(CODE_PART_OWNER_REQUIRED, "담당자 ID는 필수입니다");
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
            throw new DomainException(CODE_PART_OWNER_TEAM_REQUIRED, "담당 팀 ID는 필수입니다");
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

    public void assignDrawing(UUID drawingId) {
        if (drawingId == null) {
            throw new DomainException(CODE_PART_DRAWING_REQUIRED, "도면 ID는 필수입니다");
        }
        this.drawingId = drawingId;
        if (this._drawingRelation != null && !drawingId.equals(this._drawingRelation.getId())) {
            this._drawingRelation = null;
        }
    }

    public void unassignDrawing() {
        this.drawingId = null;
        this._drawingRelation = null;
    }

    public void bumpRevision() {
        this.revision = nextRevision(this.revision);
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

    private String normalizeName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_PART_NAME_TOO_LONG, "품명은 500자 이하여야 합니다");
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
            throw new DomainException(CODE_PART_CATEGORY_TOO_LONG, "카테고리는 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizeMaterial(String rawMaterial) {
        return normalizeWithMaxLength(rawMaterial, MAX_MATERIAL_LENGTH, CODE_PART_MATERIAL_TOO_LONG, "재질은 200자 이하여야 합니다");
    }

    private String normalizeUnit(String rawUnit) {
        return normalizeWithMaxLength(rawUnit, MAX_UNIT_LENGTH, CODE_PART_UNIT_TOO_LONG, "단위는 20자 이하여야 합니다");
    }

    private String normalizeDescription(String rawDescription) {
        return normalizeNullableText(rawDescription);
    }

    private String normalizeExtendedProperties(String rawExtendedProperties) {
        if (rawExtendedProperties == null || rawExtendedProperties.isBlank()) {
            return "{}";
        }
        return rawExtendedProperties.trim();
    }

    private String normalizeWithMaxLength(String rawValue, int maxLength, String code, String message) {
        String normalized = normalizeNullableText(rawValue);
        if (normalized != null && normalized.length() > maxLength) {
            throw new DomainException(code, message);
        }
        return normalized;
    }

    private String normalizeNullableText(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String nextRevision(String currentRevision) {
        if (currentRevision == null || currentRevision.isBlank()) {
            return "1";
        }

        var matcher = REVISION_SUFFIX_PATTERN.matcher(currentRevision);
        if (!matcher.matches()) {
            return currentRevision + ".1";
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);
        if (suffix.chars().allMatch(Character::isDigit)) {
            int width = suffix.length();
            int value = Integer.parseInt(suffix) + 1;
            return prefix + String.format("%0" + width + "d", value);
        }

        if ("Z".equalsIgnoreCase(suffix)) {
            return prefix + (Character.isUpperCase(suffix.charAt(0)) ? "AA" : "aa");
        }
        char next = (char) (suffix.charAt(0) + 1);
        return prefix + next;
    }
}
