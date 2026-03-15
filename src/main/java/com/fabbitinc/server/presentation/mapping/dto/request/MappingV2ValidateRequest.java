package com.fabbitinc.server.presentation.mapping.dto.request;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "V2 매핑 검증 요청")
public record MappingV2ValidateRequest(
        @NotNull @Schema(description = "업로드 완료된 파일 ID")
        UUID fileId,
        @Schema(description = "Excel 시트명")
        String sheetName,
        @Valid @NotNull @Schema(description = "검증 대상 V2 매핑")
        MappingV2ResultDto mapping
) {
}
