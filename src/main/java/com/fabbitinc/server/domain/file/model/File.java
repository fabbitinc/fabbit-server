package com.fabbitinc.server.domain.file.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
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

    public static final String CODE_FILE_ORIGINAL_NAME_REQUIRED = "FILE_ORIGINAL_NAME_REQUIRED";
    public static final String CODE_FILE_KEY_REQUIRED = "FILE_KEY_REQUIRED";
    public static final String CODE_FILE_CONTENT_TYPE_REQUIRED = "FILE_CONTENT_TYPE_REQUIRED";
    public static final String CODE_FILE_SIZE_INVALID = "FILE_SIZE_INVALID";
    public static final String CODE_FILE_OWNER_TYPE_REQUIRED = "FILE_OWNER_TYPE_REQUIRED";
    public static final String CODE_FILE_OWNER_ID_REQUIRED = "FILE_OWNER_ID_REQUIRED";

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
        this.originalName = requireOriginalName(originalName);
        this.fileKey = requireFileKey(fileKey);
        this.contentType = requireContentType(contentType);
        this.fileSize = requireFileSize(fileSize);
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

    public static File create(UUID id, String originalName, String fileKey, String contentType, long fileSize) {
        return new File(id, originalName, fileKey, contentType, fileSize);
    }

    public static File create(String originalName, String fileKey, String contentType, long fileSize) {
        return new File(originalName, fileKey, contentType, fileSize);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAttachable() {
        return status == FileStatus.UPLOADED && ownerId == null && !isDeleted();
    }

    public void assignOwner(String ownerType, UUID ownerId) {
        if (ownerType == null || ownerType.isBlank()) {
            throw new DomainException(CODE_FILE_OWNER_TYPE_REQUIRED, "소유자 타입은 필수입니다");
        }
        if (ownerId == null) {
            throw new DomainException(CODE_FILE_OWNER_ID_REQUIRED, "소유자 ID는 필수입니다");
        }
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

    private String requireOriginalName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_FILE_ORIGINAL_NAME_REQUIRED, "원본 파일명은 필수입니다");
        }
        return value;
    }

    private String requireFileKey(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_FILE_KEY_REQUIRED, "파일 키는 필수입니다");
        }
        return value;
    }

    private String requireContentType(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_FILE_CONTENT_TYPE_REQUIRED, "콘텐츠 타입은 필수입니다");
        }
        return value;
    }

    private long requireFileSize(long value) {
        if (value < 0) {
            throw new DomainException(CODE_FILE_SIZE_INVALID, "파일 크기는 0 이상이어야 합니다");
        }
        return value;
    }
}
