package com.fabbitinc.server.presentation.migration.request;

import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorManifestFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Inventor 마이그레이션 시작 요청")
public record StartInventorMigrationRequest(
        @Schema(description = "프로젝트 이름", example = "Motor Assembly")
        @NotBlank(message = "projectName은 필수입니다")
        @Size(max = 200, message = "projectName은 200자 이하여야 합니다")
        String projectName,

        @Schema(description = "IPJ 파일 경로", example = "Motor Assembly.ipj")
        @NotBlank(message = "ipjPath는 필수입니다")
        @Size(max = 1000, message = "ipjPath는 1000자 이하여야 합니다")
        String ipjPath,

        @Schema(description = "Inventor 버전", example = "2024")
        @Size(max = 50, message = "inventorVersion은 50자 이하여야 합니다")
        String inventorVersion,

        @Schema(description = "매니페스트 파일 목록")
        @NotEmpty(message = "files는 1개 이상이어야 합니다")
        List<@Valid FileItemRequest> files
) {
    @Schema(description = "매니페스트 파일 항목")
    public record FileItemRequest(
            @Schema(description = "파일 상대 경로", example = "Parts/Shaft.ipt")
            @NotBlank(message = "path는 필수입니다")
            @Size(max = 1000, message = "path는 1000자 이하여야 합니다")
            String path,

            @Schema(description = "원본 파일명, 생략 시 path의 마지막 세그먼트를 사용합니다", example = "Shaft.ipt")
            @Size(max = 500, message = "originalName은 500자 이하여야 합니다")
            String originalName,

            @Schema(description = "매니페스트 파일 타입", example = "PART")
            @NotNull(message = "type은 필수입니다")
            InventorManifestFileType type,

            @Schema(description = "콘텐츠 타입, 생략 시 application/octet-stream", example = "application/octet-stream")
            @Size(max = 100, message = "contentType은 100자 이하여야 합니다")
            String contentType,

            @Schema(description = "파일 크기(bytes)", example = "245760")
            @Positive(message = "sizeBytes는 0보다 커야 합니다")
            long sizeBytes,

            @Schema(description = "SHA-256 hex, 선택값", example = "6d2bc3f13b59bf38368ffce5aa7498479f880c6da14961fb1bc696ff44e43173")
            @Pattern(regexp = "^$|^[0-9A-Fa-f]{64}$", message = "contentHash는 SHA-256 hex 형식이어야 합니다")
            String contentHash
    ) {
        public InventorManifestFile toManifestFile() {
            return new InventorManifestFile(path, originalName, type, contentType, sizeBytes, contentHash);
        }
    }
}
