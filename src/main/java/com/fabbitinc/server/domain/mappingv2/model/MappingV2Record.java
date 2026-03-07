package com.fabbitinc.server.domain.mappingv2.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "mapping_v2_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_mapping_v2_records_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "ix_mapping_v2_records_is_active", columnList = "is_active")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MappingV2Record extends AbstractAuditableEntity {

    public static final String CODE_MAPPING_V2_RECORD_NAME_REQUIRED = "MAPPING_V2_RECORD_NAME_REQUIRED";
    public static final String CODE_MAPPING_V2_RECORD_NAME_TOO_LONG = "MAPPING_V2_RECORD_NAME_TOO_LONG";
    public static final String CODE_MAPPING_V2_RECORD_USAGE_INCREMENT_INVALID =
            "MAPPING_V2_RECORD_USAGE_INCREMENT_INVALID";

    private static final int MAX_NAME_LENGTH = 200;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @OneToMany(mappedBy = "record", fetch = FetchType.LAZY)
    private List<MappingV2Revision> revisions = new ArrayList<>();

    private MappingV2Record(String name) {
        super(UuidV7Generator.next());
        this.name = requireName(name);
        this.active = true;
        this.usageCount = 0;
    }

    public static MappingV2Record create(String name) {
        return new MappingV2Record(name);
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void incrementUsage(int amount) {
        if (amount <= 0) {
            throw new DomainException(
                    CODE_MAPPING_V2_RECORD_USAGE_INCREMENT_INVALID,
                    "사용량 증가는 1 이상이어야 합니다"
            );
        }
        this.usageCount += amount;
    }

    public MappingV2Revision createRevision(
            UUID fileId,
            String sheetName,
            String originalHeaders,
            String mapping
    ) {
        MappingV2Revision revision = MappingV2Revision.create(
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

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_MAPPING_V2_RECORD_NAME_REQUIRED, "매핑 이름은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_MAPPING_V2_RECORD_NAME_TOO_LONG, "매핑 이름은 200자 이하여야 합니다");
        }
        return trimmed;
    }

    private int nextVersion() {
        return revisions.stream()
                .mapToInt(MappingV2Revision::getVersion)
                .max()
                .orElse(0) + 1;
    }
}
