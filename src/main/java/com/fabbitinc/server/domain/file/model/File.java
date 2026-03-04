package com.fabbitinc.server.domain.file.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "files",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_files_file_key", columnNames = "file_key")
        },
        indexes = {
                @Index(name = "ix_files_owner_type_owner_id", columnList = "owner_type,owner_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends AbstractCreatedEntity {

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "owner_type", length = 50)
    private String ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public File(
            UUID id,
            String originalName,
            String fileKey,
            String contentType,
            long fileSize
    ) {
        super(id);
        this.originalName = originalName;
        this.fileKey = fileKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.status = FileStatus.PENDING;
    }

    public File(
            String originalName,
            String fileKey,
            String contentType,
            long fileSize
    ) {
        this(UuidV7Generator.next(), originalName, fileKey, contentType, fileSize);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAttachable() {
        return status == FileStatus.UPLOADED && ownerId == null && !isDeleted();
    }

    public void assignOwner(String ownerType, UUID ownerId) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    public void changeToThumbnailWebp() {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }

        int extStart = fileKey.lastIndexOf('.');
        if (extStart > -1) {
            this.fileKey = fileKey.substring(0, extStart) + ".webp";
        } else {
            this.fileKey = fileKey + ".webp";
        }
        this.contentType = "image/webp";
    }

    public void markUploaded() {
        this.status = FileStatus.UPLOADED;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }
}
