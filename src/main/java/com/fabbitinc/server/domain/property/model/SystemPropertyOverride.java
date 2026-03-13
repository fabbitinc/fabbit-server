package com.fabbitinc.server.domain.property.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "system_property_overrides",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_system_property_overrides_owner_type_property_key",
                        columnNames = {"owner_type", "property_key"}
                )
        },
        indexes = {
                @Index(
                        name = "ix_system_property_overrides_owner_type_is_active_display_order",
                        columnList = "owner_type,is_active,display_order"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemPropertyOverride extends AbstractAuditableEntity {

    public static final String CODE_SYSTEM_PROPERTY_OVERRIDE_OWNER_TYPE_REQUIRED =
            "SYSTEM_PROPERTY_OVERRIDE_OWNER_TYPE_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_REQUIRED =
            "SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_REQUIRED";
    public static final String CODE_SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_TOO_LONG =
            "SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_TOO_LONG";
    public static final String CODE_SYSTEM_PROPERTY_OVERRIDE_DISPLAY_NAME_TOO_LONG =
            "SYSTEM_PROPERTY_OVERRIDE_DISPLAY_NAME_TOO_LONG";
    public static final String CODE_SYSTEM_PROPERTY_OVERRIDE_DISPLAY_ORDER_INVALID =
            "SYSTEM_PROPERTY_OVERRIDE_DISPLAY_ORDER_INVALID";

    private static final int MAX_PROPERTY_KEY_LENGTH = 100;
    private static final int MAX_DISPLAY_NAME_LENGTH = 200;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private PropertyOwnerType ownerType;

    @Column(name = "property_key", nullable = false, length = 100)
    private String propertyKey;

    @Column(name = "display_name_override", length = 200)
    private String displayNameOverride;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    private SystemPropertyOverride(
            PropertyOwnerType ownerType,
            String propertyKey,
            String displayNameOverride,
            Integer displayOrder,
            boolean active
    ) {
        super(UuidV7Generator.next());
        this.ownerType = requireOwnerType(ownerType);
        this.propertyKey = requirePropertyKey(propertyKey);
        this.displayNameOverride = normalizeDisplayNameOverride(displayNameOverride);
        this.displayOrder = normalizeDisplayOrder(displayOrder);
        this.active = active;
    }

    public static SystemPropertyOverride create(
            PropertyOwnerType ownerType,
            String propertyKey,
            String displayNameOverride,
            Integer displayOrder,
            boolean active
    ) {
        return new SystemPropertyOverride(ownerType, propertyKey, displayNameOverride, displayOrder, active);
    }

    public void changeDisplayNameOverride(String displayNameOverride) {
        this.displayNameOverride = normalizeDisplayNameOverride(displayNameOverride);
    }

    public void clearDisplayNameOverride() {
        this.displayNameOverride = null;
    }

    public void changeDisplayOrder(Integer displayOrder) {
        this.displayOrder = normalizeDisplayOrder(displayOrder);
    }

    public void clearDisplayOrder() {
        this.displayOrder = null;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private PropertyOwnerType requireOwnerType(PropertyOwnerType ownerType) {
        if (ownerType == null) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_OVERRIDE_OWNER_TYPE_REQUIRED,
                    "시스템 속성 override의 소유 타입은 필수입니다"
            );
        }
        return ownerType;
    }

    private String requirePropertyKey(String propertyKey) {
        if (propertyKey == null || propertyKey.isBlank()) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_REQUIRED,
                    "시스템 속성 override의 property_key는 필수입니다"
            );
        }

        String trimmed = propertyKey.trim();
        if (trimmed.length() > MAX_PROPERTY_KEY_LENGTH) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_TOO_LONG,
                    "시스템 속성 override의 property_key는 100자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private String normalizeDisplayNameOverride(String displayNameOverride) {
        if (displayNameOverride == null) {
            return null;
        }
        String trimmed = displayNameOverride.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_OVERRIDE_DISPLAY_NAME_TOO_LONG,
                    "시스템 속성 override의 표시명은 200자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private Integer normalizeDisplayOrder(Integer displayOrder) {
        if (displayOrder == null) {
            return null;
        }
        if (displayOrder < 0) {
            throw new DomainException(
                    CODE_SYSTEM_PROPERTY_OVERRIDE_DISPLAY_ORDER_INVALID,
                    "시스템 속성 override의 display_order는 0 이상이어야 합니다"
            );
        }
        return displayOrder;
    }
}
