package com.fabbitinc.server.presentation.mapping.dto.request;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "매핑 수정 요청")
public record MappingUpdateRequest(
        @NotNull @Schema(description = "업로드 완료된 파일 ID")
        UUID fileId,
        @Size(max = 200) @Schema(description = "매핑 이름(변경 시 전달)")
        String name,
        @Schema(description = "Excel 시트명")
        String sheetName,
        @Valid @NotNull @Schema(description = "수정 매핑")
        MappingResultDto mapping
) {
}
