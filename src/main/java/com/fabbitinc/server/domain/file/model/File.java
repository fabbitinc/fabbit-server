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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "files",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_files_file_key", columnNames = "file_key")
        },
        indexes = {
                @Index(name = "ix_files_owner_type_owner_id", columnList = "owner_type,owner_id"),
                @Index(name = "ix_files_original_name_file_size_content_hash", columnList = "original_name,file_size,content_hash")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends AbstractCreatedEntity {

    public static final String CODE_FILE_ORIGINAL_NAME_REQUIRED = "FILE_ORIGINAL_NAME_REQUIRED";
    public static final String CODE_FILE_KEY_REQUIRED = "FILE_KEY_REQUIRED";
    public static final String CODE_FILE_CONTENT_TYPE_REQUIRED = "FILE_CONTENT_TYPE_REQUIRED";
    public static final String CODE_FILE_SIZE_INVALID = "FILE_SIZE_INVALID";
    public static final String CODE_FILE_CONTENT_HASH_INVALID = "FILE_CONTENT_HASH_INVALID";
    public static final String CODE_FILE_OWNER_TYPE_REQUIRED = "FILE_OWNER_TYPE_REQUIRED";
    public static final String CODE_FILE_OWNER_ID_REQUIRED = "FILE_OWNER_ID_REQUIRED";
    public static final String CODE_FILE_NOT_ATTACHABLE = "FILE_NOT_ATTACHABLE";

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "owner_type", length = 50)
    private String ownerType;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private File(
            UUID id,
            String originalName,
            String fileKey,
            String contentType,
            long fileSize,
            String contentHash
    ) {
        super(id);
        this.originalName = requireOriginalName(originalName);
        this.fileKey = requireFileKey(fileKey);
        this.contentType = requireContentType(contentType);
        this.fileSize = requireFileSize(fileSize);
        this.contentHash = normalizeContentHash(contentHash);
        this.status = FileStatus.PENDING;
    }

    private File(
            String originalName,
            String fileKey,
            String contentType,
            long fileSize,
            String contentHash
    ) {
        this(UuidV7Generator.next(), originalName, fileKey, contentType, fileSize, contentHash);
    }

    public static File create(
            UUID id,
            String originalName,
            String fileKey,
            String contentType,
            long fileSize,
            String contentHash
    ) {
        return new File(id, originalName, fileKey, contentType, fileSize, contentHash);
    }

    public static File create(
            UUID id,
            String originalName,
            String fileKey,
            String contentType,
            long fileSize
    ) {
        return new File(id, originalName, fileKey, contentType, fileSize, null);
    }

    public static File create(
            String originalName,
            String fileKey,
            String contentType,
            long fileSize,
            String contentHash
    ) {
        return new File(originalName, fileKey, contentType, fileSize, contentHash);
    }

    public static File create(
            String originalName,
            String fileKey,
            String contentType,
            long fileSize
    ) {
        return new File(originalName, fileKey, contentType, fileSize, null);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAttachable() {
        return status == FileStatus.UPLOADED && ownerId == null && !isDeleted();
    }

    public void assignOwner(String ownerType, UUID ownerId) {
        String normalizedOwnerType = normalizeRequiredText(ownerType, CODE_FILE_OWNER_TYPE_REQUIRED, "소유자 타입은 필수입니다");
        if (ownerId == null) {
            throw new DomainException(CODE_FILE_OWNER_ID_REQUIRED, "소유자 ID는 필수입니다");
        }
        if (normalizedOwnerType.equals(this.ownerType) && ownerId.equals(this.ownerId)) {
            return;
        }
        if (!isAttachable()) {
            throw new DomainException(CODE_FILE_NOT_ATTACHABLE, "업로드 완료되고 아직 연결되지 않은 파일만 소유자를 지정할 수 있습니다");
        }
        this.ownerType = normalizedOwnerType;
        this.ownerId = ownerId;
    }

    public void changeStoredObject(String fileKey, String contentType, long fileSize) {
        this.fileKey = requireFileKey(fileKey);
        this.contentType = requireContentType(contentType);
        this.fileSize = requireFileSize(fileSize);
    }

    public void markUploaded() {
        this.status = FileStatus.UPLOADED;
    }

    public void softDelete(UUID actorId) {
        this.deletedAt = Instant.now();
    }

    private String requireOriginalName(String value) {
        return normalizeRequiredText(value, CODE_FILE_ORIGINAL_NAME_REQUIRED, "원본 파일명은 필수입니다");
    }

    private String requireFileKey(String value) {
        return normalizeRequiredText(value, CODE_FILE_KEY_REQUIRED, "파일 키는 필수입니다");
    }

    private String requireContentType(String value) {
        return normalizeRequiredText(value, CODE_FILE_CONTENT_TYPE_REQUIRED, "콘텐츠 타입은 필수입니다");
    }

    private String normalizeRequiredText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(code, message);
        }
        return value.trim();
    }

    private long requireFileSize(long value) {
        if (value < 0) {
            throw new DomainException(CODE_FILE_SIZE_INVALID, "파일 크기는 0 이상이어야 합니다");
        }
        return value;
    }

    private String normalizeContentHash(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if (!normalized.matches("^[0-9a-f]{64}$")) {
            throw new DomainException(CODE_FILE_CONTENT_HASH_INVALID, "content hash는 SHA-256 hex 형식이어야 합니다");
        }
        return normalized;
    }
}
