package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "parts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_parts_part_number", columnNames = "part_number")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Part extends AbstractCreatedEntity {

    @Column(name = "drawing_id")
    private UUID drawingId;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "owner_team_id")
    private UUID ownerTeamId;

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

    @Column(name = "lifecycle_state", length = 50)
    private String lifecycleState;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "extended_properties", nullable = false, columnDefinition = "jsonb")
    private String extendedProperties = "{}";

    public Part(String partNumber) {
        this(partNumber, null);
    }

    public Part(String partNumber, String name) {
        super(UuidV7Generator.next());
        this.partNumber = partNumber;
        this.name = name;
        this.revision = "1";
        this.extendedProperties = "{}";
    }

    public void changeCategory(String category) {
        this.category = category;
    }

    public void assignOwner(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void unassignOwner() {
        this.ownerId = null;
    }

    public void assignOwnerTeam(UUID ownerTeamId) {
        this.ownerTeamId = ownerTeamId;
    }

    public void unassignOwnerTeam() {
        this.ownerTeamId = null;
    }

    public void assignDrawing(UUID drawingId) {
        this.drawingId = drawingId;
    }

    public void unassignDrawing() {
        this.drawingId = null;
    }
}
