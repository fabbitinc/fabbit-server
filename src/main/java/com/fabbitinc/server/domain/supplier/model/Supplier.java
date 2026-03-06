package com.fabbitinc.server.domain.supplier.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
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
        name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_suppliers_company_name", columnNames = "company_name")
        },
        indexes = {
                @Index(name = "ix_suppliers_code", columnList = "code")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier extends AbstractAuditableEntity {

    public static final String CODE_SUPPLIER_COMPANY_NAME_REQUIRED = "SUPPLIER_COMPANY_NAME_REQUIRED";
    public static final String CODE_SUPPLIER_COMPANY_NAME_TOO_LONG = "SUPPLIER_COMPANY_NAME_TOO_LONG";
    public static final String CODE_SUPPLIER_CODE_TOO_LONG = "SUPPLIER_CODE_TOO_LONG";
    public static final String CODE_SUPPLIER_COUNTRY_TOO_LONG = "SUPPLIER_COUNTRY_TOO_LONG";

    private static final int MAX_COMPANY_NAME_LENGTH = 200;
    private static final int MAX_CODE_LENGTH = 100;
    private static final int MAX_COUNTRY_LENGTH = 100;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "contact_info", columnDefinition = "text")
    private String contactInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    public Supplier(
            UUID id,
            String companyName,
            String code,
            String country,
            String contactInfo,
            String extendedProperties
    ) {
        super(id);
        this.companyName = requireCompanyName(companyName);
        this.code = normalizeCode(code);
        this.country = normalizeCountry(country);
        this.contactInfo = normalizeNullableText(contactInfo);
        this.extendedProperties = normalizeExtendedProperties(extendedProperties);
    }

    public Supplier(
            String companyName,
            String code,
            String country,
            String contactInfo,
            String extendedProperties
    ) {
        this(UuidV7Generator.next(), companyName, code, country, contactInfo, extendedProperties);
    }

    public static Supplier create(
            String companyName,
            String code,
            String country,
            String contactInfo,
            String extendedProperties
    ) {
        return new Supplier(companyName, code, country, contactInfo, extendedProperties);
    }

    private String requireCompanyName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_SUPPLIER_COMPANY_NAME_REQUIRED, "공급사명은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_COMPANY_NAME_LENGTH) {
            throw new DomainException(CODE_SUPPLIER_COMPANY_NAME_TOO_LONG, "공급사명은 200자 이하여야 합니다");
        }
        return trimmed;
    }

    private String normalizeCode(String value) {
        String normalized = normalizeNullableText(value);
        if (normalized != null && normalized.length() > MAX_CODE_LENGTH) {
            throw new DomainException(CODE_SUPPLIER_CODE_TOO_LONG, "공급사 코드는 100자 이하여야 합니다");
        }
        return normalized;
    }

    private String normalizeCountry(String value) {
        String normalized = normalizeNullableText(value);
        if (normalized != null && normalized.length() > MAX_COUNTRY_LENGTH) {
            throw new DomainException(CODE_SUPPLIER_COUNTRY_TOO_LONG, "국가는 100자 이하여야 합니다");
        }
        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeExtendedProperties(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value.trim();
    }
}
