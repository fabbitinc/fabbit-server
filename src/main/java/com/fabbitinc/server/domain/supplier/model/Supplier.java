package com.fabbitinc.server.domain.supplier.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "contact_info", columnDefinition = "text")
    private String contactInfo;

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
        this.companyName = companyName;
        this.code = code;
        this.country = country;
        this.contactInfo = contactInfo;
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

    private String normalizeExtendedProperties(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }
        return value;
    }
}
