package com.fabbitinc.server.application.mapping.dto.request;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "매핑 검증 요청")
public record MappingValidateRequest(
        @NotNull @Schema(description = "업로드 완료된 파일 ID")
        UUID fileId,
        @Schema(description = "Excel 시트명")
        String sheetName,
        @Valid @NotNull @Schema(description = "검증 대상 매핑")
        MappingResultDto mapping
) {
}
