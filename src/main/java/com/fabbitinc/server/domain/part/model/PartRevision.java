package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    public static final String CODE_PART_REVISION_PART_REQUIRED = "PART_REVISION_PART_REQUIRED";
    public static final String CODE_PART_REVISION_SYNTHESIS_JOB_REQUIRED = "PART_REVISION_SYNTHESIS_JOB_REQUIRED";

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", insertable = false, updatable = false)
    private Part part;

    @Column(name = "synthesis_job_id")
    private UUID synthesisJobId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "synthesis_job_id", insertable = false, updatable = false)
    private SynthesisJob _synthesisJobRelation;

    @Column(name = "drawing_id")
    private UUID drawingId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drawing_id", insertable = false, updatable = false)
    private Drawing _drawingRelation;

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

    @Convert(converter = PartLifecycleStateConverter.class)
    @Column(name = "lifecycle_state", length = 50)
    private PartLifecycleState lifecycleState;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties;

    private PartRevision(Part part, UUID synthesisJobId) {
        super(UuidV7Generator.next());
        Part requiredPart = requirePart(part);
        this.partId = requiredPart.getId();
        this.part = requiredPart;
        this.synthesisJobId = synthesisJobId;
        this.drawingId = requiredPart.getDrawingId();
        this._drawingRelation = null;
        this.partNumber = requiredPart.getPartNumber();
        this.name = requiredPart.getName();
        this.revision = requiredPart.getRevision();
        this.material = requiredPart.getMaterial();
        this.unit = requiredPart.getUnit();
        this.description = requiredPart.getDescription();
        this.category = requiredPart.getCategory();
        this.phantom = requiredPart.getPhantom();
        this.lifecycleState = requiredPart.getLifecycleState();
        this.leadTimeDays = requiredPart.getLeadTimeDays();
        this.extendedProperties = requiredPart.getExtendedProperties() == null || requiredPart.getExtendedProperties().isBlank()
                ? "{}"
                : requiredPart.getExtendedProperties();
    }

    public static PartRevision capture(Part part, UUID synthesisJobId) {
        return new PartRevision(part, synthesisJobId);
    }

    private Part requirePart(Part value) {
        if (value == null) {
            throw new DomainException(CODE_PART_REVISION_PART_REQUIRED, "파트는 필수입니다");
        }
        return value;
    }
}
