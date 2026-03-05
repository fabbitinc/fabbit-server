package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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

@Getter
@Entity
@Table(
        name = "part_revisions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_revisions_part_id_revision",
                        columnNames = {"part_id", "revision"}
                )
        },
        indexes = {
                @Index(name = "ix_part_revisions_part_id", columnList = "part_id"),
                @Index(name = "ix_part_revisions_synthesis_job_id", columnList = "synthesis_job_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartRevision extends AbstractCreatedEntity {

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", insertable = false, updatable = false)
    private Part part;

    @Column(name = "synthesis_job_id")
    private UUID synthesisJobId;

    @Column(name = "drawing_id")
    private UUID drawingId;

    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "revision", nullable = false, length = 50)
    private String revision;

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

    @Column(name = "lifecycle_state", length = 50)
    private String lifecycleState;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    private PartRevision(Part part, UUID synthesisJobId) {
        super(UuidV7Generator.next());
        this.partId = part.getId();
        this.synthesisJobId = synthesisJobId;
        this.drawingId = part.getDrawingId();
        this.partNumber = part.getPartNumber();
        this.name = part.getName();
        this.revision = part.getRevision();
        this.material = part.getMaterial();
        this.unit = part.getUnit();
        this.description = part.getDescription();
        this.category = part.getCategory();
        this.phantom = part.getPhantom();
        this.lifecycleState = part.getLifecycleState();
        this.leadTimeDays = part.getLeadTimeDays();
        this.extendedProperties = part.getExtendedProperties() == null || part.getExtendedProperties().isBlank()
                ? "{}"
                : part.getExtendedProperties();
    }

    public static PartRevision capture(Part part, UUID synthesisJobId) {
        return new PartRevision(part, synthesisJobId);
    }
}
