package com.fabbitinc.server.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractIdEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    protected AbstractIdEntity(UUID id) {
        this.id = Objects.requireNonNull(id);
    }
}
