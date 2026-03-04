package com.fabbitinc.server.domain.project.model;

import com.fabbitinc.server.domain.common.entity.AbstractSoftDeletableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends AbstractSoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    public Project(String name, String description) {
        super(UuidV7Generator.next());
        this.name = name;
        this.description = description;
        this.archived = false;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }
}
