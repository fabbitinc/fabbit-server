package com.fabbitinc.server.domain.mapping.model;

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

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    public MappingRecord(String name, String scope) {
        super(UuidV7Generator.next());
        this.name = name;
        this.scope = scope;
        this.active = true;
        this.usageCount = 0;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void updateScope(String scope) {
        this.scope = scope;
    }

    public void deactivate() {
        this.active = false;
    }

    public void incrementUsage(int amount) {
        this.usageCount += amount;
    }
}
