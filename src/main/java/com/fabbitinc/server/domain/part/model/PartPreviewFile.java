package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "part_preview_files",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_part_preview_files_file_id", columnNames = "file_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartPreviewFile extends AbstractCreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_preview_id", nullable = false)
    private PartPreview partPreview;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    private PartPreviewFile(PartPreview partPreview, UUID fileId) {
        super(UuidV7Generator.next());
        this.partPreview = requirePartPreview(partPreview);
        this.fileId = requireFileId(fileId);
    }

    public static PartPreviewFile create(PartPreview partPreview, UUID fileId) {
        return new PartPreviewFile(partPreview, fileId);
    }

    private PartPreview requirePartPreview(PartPreview value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_FILE_PREVIEW_REQUIRED", "대표 미리보기는 필수입니다");
        }
        return value;
    }

    private UUID requireFileId(UUID value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_FILE_FILE_REQUIRED", "미리보기 파일 ID는 필수입니다");
        }
        return value;
    }
}
