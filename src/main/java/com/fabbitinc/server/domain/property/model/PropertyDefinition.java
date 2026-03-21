package com.fabbitinc.server.domain.property.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "property_definitions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_property_definitions_owner_type_property_key",
                        columnNames = {"owner_type", "property_key"}
                )
        },
        indexes = {
                @Index(
                        name = "ix_property_definitions_owner_type_is_active_display_order",
                        columnList = "owner_type,is_active,display_order"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyDefinition extends AbstractAuditableEntity {

    public static final String CODE_PROPERTY_DEFINITION_OWNER_TYPE_REQUIRED =
            "PROPERTY_DEFINITION_OWNER_TYPE_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_PROPERTY_KEY_REQUIRED =
            "PROPERTY_DEFINITION_PROPERTY_KEY_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_PROPERTY_KEY_TOO_LONG =
            "PROPERTY_DEFINITION_PROPERTY_KEY_TOO_LONG";
    public static final String CODE_PROPERTY_DEFINITION_SOURCE_TYPE_REQUIRED =
            "PROPERTY_DEFINITION_SOURCE_TYPE_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_STORAGE_KIND_REQUIRED =
            "PROPERTY_DEFINITION_STORAGE_KIND_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_STORAGE_BINDING_REQUIRED =
            "PROPERTY_DEFINITION_STORAGE_BINDING_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_STORAGE_BINDING_TOO_LONG =
            "PROPERTY_DEFINITION_STORAGE_BINDING_TOO_LONG";
    public static final String CODE_PROPERTY_DEFINITION_DISPLAY_NAME_REQUIRED =
            "PROPERTY_DEFINITION_DISPLAY_NAME_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_DISPLAY_NAME_TOO_LONG =
            "PROPERTY_DEFINITION_DISPLAY_NAME_TOO_LONG";
    public static final String CODE_PROPERTY_DEFINITION_VALUE_TYPE_REQUIRED =
            "PROPERTY_DEFINITION_VALUE_TYPE_REQUIRED";
    public static final String CODE_PROPERTY_DEFINITION_OPTION_MODE_NOT_ALLOWED =
            "PROPERTY_DEFINITION_OPTION_MODE_NOT_ALLOWED";
    public static final String CODE_PROPERTY_DEFINITION_OPTIONS_NOT_ALLOWED =
            "PROPERTY_DEFINITION_OPTIONS_NOT_ALLOWED";
    public static final String CODE_PROPERTY_DEFINITION_DUPLICATE_OPTION_VALUE =
            "PROPERTY_DEFINITION_DUPLICATE_OPTION_VALUE";
    public static final String CODE_PROPERTY_DEFINITION_DISPLAY_ORDER_INVALID =
            "PROPERTY_DEFINITION_DISPLAY_ORDER_INVALID";
    public static final String CODE_PROPERTY_DEFINITION_SYSTEM_ACTIVE_NOT_CONFIGURABLE =
            "PROPERTY_DEFINITION_SYSTEM_ACTIVE_NOT_CONFIGURABLE";
    public static final String CODE_PROPERTY_DEFINITION_SYSTEM_SHAPE_IMMUTABLE =
            "PROPERTY_DEFINITION_SYSTEM_SHAPE_IMMUTABLE";

    private static final int MAX_PROPERTY_KEY_LENGTH = 100;
    private static final int MAX_STORAGE_BINDING_LENGTH = 200;
    private static final int MAX_DISPLAY_NAME_LENGTH = 200;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private PropertyOwnerType ownerType;

    @Column(name = "property_key", nullable = false, length = 100)
    private String propertyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private PropertySourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_kind", nullable = false, length = 30)
    private PropertyStorageKind storageKind;

    @Column(name = "storage_binding", nullable = false, length = 200)
    private String storageBinding;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_system_property_kind", length = 50)
    private PartSystemPropertyKind partSystemPropertyKind;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private PropertyValueType valueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_mode", length = 20)
    private PropertyOptionMode optionMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", nullable = false, columnDefinition = "jsonb")
    private List<PropertyOptionItem> options = List.of();

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_active_configurable", nullable = false)
    private boolean activeConfigurable;

    private PropertyDefinition(
            UUID id,
            PropertyOwnerType ownerType,
            String propertyKey,
            PropertySourceType sourceType,
            PropertyStorageKind storageKind,
            String storageBinding,
            PartSystemPropertyKind partSystemPropertyKind,
            String displayName,
            String description,
            PropertyValueType valueType,
            PropertyOptionMode optionMode,
            List<PropertyOptionItem> options,
            int displayOrder,
            boolean required,
            boolean active,
            boolean activeConfigurable
    ) {
        super(id);
        this.ownerType = requireOwnerType(ownerType);
        this.propertyKey = requirePropertyKey(propertyKey);
        this.sourceType = requireSourceType(sourceType);
        this.storageKind = requireStorageKind(storageKind);
        this.storageBinding = requireStorageBinding(storageBinding);
        this.partSystemPropertyKind = normalizePartSystemPropertyKind(
                partSystemPropertyKind,
                this.ownerType,
                this.sourceType
        );
        this.displayName = requireDisplayName(displayName);
        this.description = normalizeDescription(description);
        this.valueType = requireValueType(valueType);
        this.optionMode = normalizeOptionMode(optionMode, this.valueType);
        this.options = normalizeOptions(options, this.valueType);
        this.displayOrder = requireDisplayOrder(displayOrder);
        this.required = required;
        this.active = active;
        this.activeConfigurable = activeConfigurable;
    }

    public static PropertyDefinition defineCustomProperty(
            PropertyOwnerType ownerType,
            String displayName,
            String description,
            PropertyValueType valueType,
            PropertyOptionMode optionMode,
            List<PropertyOptionItem> options,
            int displayOrder,
            boolean required
    ) {
        UUID id = UuidV7Generator.next();
        return new PropertyDefinition(
                id,
                ownerType,
                id.toString(),
                PropertySourceType.CUSTOM,
                PropertyStorageKind.EXTENDED_PROPERTY,
                id.toString(),
                null,
                displayName,
                description,
                valueType,
                optionMode,
                options,
                displayOrder,
                required,
                true,
                true
        );
    }

    public static PropertyDefinition defineSystemProperty(
            PropertyOwnerType ownerType,
            String propertyKey,
            PartSystemPropertyKind partSystemPropertyKind,
            String displayName,
            String description,
            PropertyValueType valueType,
            PropertyOptionMode optionMode,
            List<PropertyOptionItem> options,
            String storageBinding,
            int displayOrder,
            boolean required,
            boolean activeConfigurable
    ) {
        return new PropertyDefinition(
                UuidV7Generator.next(),
                ownerType,
                propertyKey,
                PropertySourceType.SYSTEM,
                PropertyStorageKind.COLUMN,
                storageBinding,
                partSystemPropertyKind,
                displayName,
                description,
                valueType,
                optionMode,
                options,
                displayOrder,
                required,
                true,
                activeConfigurable
        );
    }

    public boolean isSystemProperty() {
        return sourceType.isSystem();
    }

    public void renameDisplayName(String displayName) {
        this.displayName = requireDisplayName(displayName);
    }

    public void changeDescription(String description) {
        this.description = normalizeDescription(description);
    }

    public void changeValueType(PropertyValueType valueType) {
        validateShapeMutable();
        PropertyValueType normalizedValueType = requireValueType(valueType);
        normalizeOptionMode(this.optionMode, normalizedValueType);
        validateOptions(this.options, normalizedValueType);
        this.valueType = normalizedValueType;
    }

    public void reconfigureValueSpec(
            PropertyValueType valueType,
            PropertyOptionMode optionMode,
            List<PropertyOptionItem> options
    ) {
        validateShapeMutable();
        PropertyValueType normalizedValueType = requireValueType(valueType);
        PropertyOptionMode normalizedOptionMode = normalizeOptionMode(optionMode, normalizedValueType);
        List<PropertyOptionItem> normalizedOptions = normalizeOptions(options, normalizedValueType);
        this.valueType = normalizedValueType;
        this.optionMode = normalizedOptionMode;
        this.options = normalizedOptions;
    }

    public void changeOptionMode(PropertyOptionMode optionMode) {
        validateShapeMutable();
        this.optionMode = normalizeOptionMode(optionMode, this.valueType);
    }

    public void changeOptions(List<PropertyOptionItem> options) {
        validateShapeMutable();
        this.options = normalizeOptions(options, this.valueType);
    }

    public void reorder(int displayOrder) {
        this.displayOrder = requireDisplayOrder(displayOrder);
    }

    public void markRequired() {
        validateShapeMutable();
        this.required = true;
    }

    public void markOptional() {
        validateShapeMutable();
        this.required = false;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        if (sourceType.isSystem() && !activeConfigurable) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_SYSTEM_ACTIVE_NOT_CONFIGURABLE,
                    "비활성화할 수 없는 시스템 속성입니다"
            );
        }
        this.active = false;
    }

    public void applySystemProvisioning(
            PartSystemPropertyKind partSystemPropertyKind,
            String description,
            PropertyValueType valueType,
            PropertyOptionMode optionMode,
            List<PropertyOptionItem> options,
            String storageBinding,
            boolean required,
            boolean activeConfigurable
    ) {
        if (!sourceType.isSystem()) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_SYSTEM_SHAPE_IMMUTABLE,
                    "시스템 속성에만 provisioning을 적용할 수 있습니다"
            );
        }

        this.partSystemPropertyKind = normalizePartSystemPropertyKind(partSystemPropertyKind, ownerType, sourceType);
        this.description = normalizeDescription(description);
        this.valueType = requireValueType(valueType);
        this.optionMode = normalizeOptionMode(optionMode, this.valueType);
        this.options = normalizeOptions(options, this.valueType);
        this.storageKind = PropertyStorageKind.COLUMN;
        this.storageBinding = requireStorageBinding(storageBinding);
        this.required = required;
        this.activeConfigurable = activeConfigurable;
    }

    private PropertyOwnerType requireOwnerType(PropertyOwnerType value) {
        if (value == null) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_OWNER_TYPE_REQUIRED,
                    "속성 소유 타입은 필수입니다"
            );
        }
        return value;
    }

    private String requirePropertyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_PROPERTY_KEY_REQUIRED,
                    "속성 key는 필수입니다"
            );
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_PROPERTY_KEY_LENGTH) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_PROPERTY_KEY_TOO_LONG,
                    "속성 key는 100자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private PropertySourceType requireSourceType(PropertySourceType value) {
        if (value == null) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_SOURCE_TYPE_REQUIRED,
                    "속성 source_type은 필수입니다"
            );
        }
        return value;
    }

    private PropertyStorageKind requireStorageKind(PropertyStorageKind value) {
        if (value == null) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_STORAGE_KIND_REQUIRED,
                    "속성 storage_kind는 필수입니다"
            );
        }
        return value;
    }

    private String requireStorageBinding(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_STORAGE_BINDING_REQUIRED,
                    "속성 storage_binding은 필수입니다"
            );
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_STORAGE_BINDING_LENGTH) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_STORAGE_BINDING_TOO_LONG,
                    "속성 storage_binding은 200자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private PropertyValueType requireValueType(PropertyValueType value) {
        if (value == null) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_VALUE_TYPE_REQUIRED,
                    "속성 값 타입은 필수입니다"
            );
        }
        return value;
    }

    private String requireDisplayName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_DISPLAY_NAME_REQUIRED,
                    "속성 표시명은 필수입니다"
            );
        }

        String trimmed = value.trim();
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_DISPLAY_NAME_TOO_LONG,
                    "속성 표시명은 200자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private PropertyOptionMode normalizeOptionMode(
            PropertyOptionMode optionMode,
            PropertyValueType valueType
    ) {
        if (valueType != PropertyValueType.OPTION) {
            if (optionMode == null) {
                return null;
            }
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_OPTION_MODE_NOT_ALLOWED,
                    "OPTION 타입이 아닌 속성은 option_mode를 가질 수 없습니다"
            );
        }

        if (optionMode == null) {
            return PropertyOptionMode.FIXED;
        }
        return optionMode;
    }

    private List<PropertyOptionItem> normalizeOptions(
            List<PropertyOptionItem> options,
            PropertyValueType valueType
    ) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<PropertyOptionItem> normalized = List.copyOf(options);
        validateOptions(normalized, valueType);
        return normalized;
    }

    private void validateOptions(List<PropertyOptionItem> options, PropertyValueType valueType) {
        if (valueType != PropertyValueType.OPTION && !options.isEmpty()) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_OPTIONS_NOT_ALLOWED,
                    "OPTION 타입이 아닌 속성은 옵션 목록을 가질 수 없습니다"
            );
        }

        Set<String> optionValues = new LinkedHashSet<>();
        for (PropertyOptionItem option : options) {
            if (!optionValues.add(option.value())) {
                throw new DomainException(
                        CODE_PROPERTY_DEFINITION_DUPLICATE_OPTION_VALUE,
                        "옵션 value는 중복될 수 없습니다"
                );
            }
        }
    }

    private int requireDisplayOrder(int value) {
        if (value < 0) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_DISPLAY_ORDER_INVALID,
                    "display_order는 0 이상이어야 합니다"
            );
        }
        return value;
    }

    private PartSystemPropertyKind normalizePartSystemPropertyKind(
            PartSystemPropertyKind value,
            PropertyOwnerType ownerType,
            PropertySourceType sourceType
    ) {
        if (!sourceType.isSystem() || ownerType != PropertyOwnerType.PART) {
            return null;
        }
        return value;
    }

    private void validateShapeMutable() {
        if (sourceType.isSystem()) {
            throw new DomainException(
                    CODE_PROPERTY_DEFINITION_SYSTEM_SHAPE_IMMUTABLE,
                    "시스템 속성의 타입/옵션/필수 여부는 변경할 수 없습니다"
            );
        }
    }
}
