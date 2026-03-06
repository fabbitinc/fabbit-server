package com.fabbitinc.server.domain.mapping.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "mapping_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_mapping_records_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "ix_mapping_records_scope_is_active", columnList = "scope,is_active")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MappingRecord extends AbstractAuditableEntity {

    public static final String CODE_MAPPING_RECORD_NAME_REQUIRED = "MAPPING_RECORD_NAME_REQUIRED";
    public static final String CODE_MAPPING_RECORD_NAME_TOO_LONG = "MAPPING_RECORD_NAME_TOO_LONG";
    public static final String CODE_MAPPING_RECORD_SCOPE_REQUIRED = "MAPPING_RECORD_SCOPE_REQUIRED";
    public static final String CODE_MAPPING_RECORD_USAGE_INCREMENT_INVALID = "MAPPING_RECORD_USAGE_INCREMENT_INVALID";

    private static final int MAX_NAME_LENGTH = 200;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private MappingScope scope;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @OneToMany(mappedBy = "record", fetch = FetchType.LAZY)
    private List<MappingRevision> revisions = new ArrayList<>();

    private MappingRecord(String name, MappingScope scope) {
        super(UuidV7Generator.next());
        this.name = requireName(name);
        this.scope = requireScope(scope);
        this.active = true;
        this.usageCount = 0;
    }

    public static MappingRecord create(String name, MappingScope scope) {
        return new MappingRecord(name, scope);
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeScope(MappingScope scope) {
        this.scope = requireScope(scope);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void incrementUsage(int amount) {
        if (amount <= 0) {
            throw new DomainException(CODE_MAPPING_RECORD_USAGE_INCREMENT_INVALID, "사용량 증가는 1 이상이어야 합니다");
        }
        this.usageCount += amount;
    }

    public MappingRevision createRevision(
            UUID fileId,
            String sheetName,
            String originalHeaders,
            String mapping
    ) {
        MappingRevision revision = MappingRevision.create(
                this,
                fileId,
                nextVersion(),
                sheetName,
                originalHeaders,
                mapping
        );
        revisions.add(revision);
        return revision;
    }

    public List<MappingRevision> getRevisions() {
        return List.copyOf(revisions);
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_MAPPING_RECORD_NAME_REQUIRED, "매핑 이름은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_MAPPING_RECORD_NAME_TOO_LONG, "매핑 이름은 200자 이하여야 합니다");
        }
        return trimmed;
    }

    private MappingScope requireScope(MappingScope value) {
        if (value == null) {
            throw new DomainException(CODE_MAPPING_RECORD_SCOPE_REQUIRED, "매핑 범위는 필수입니다");
        }
        return value;
    }

    private int nextVersion() {
        return revisions.stream()
                .mapToInt(MappingRevision::getVersion)
                .max()
                .orElse(0) + 1;
    }
}
