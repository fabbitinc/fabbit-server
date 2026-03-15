package com.fabbitinc.server.presentation.mapping.dto.request;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "V2 매핑 확정 요청")
public record MappingV2ConfirmRequest(
        @NotNull
        @Schema(description = "업로드 완료된 파일 ID")
        UUID fileId,
        @NotBlank
        @Size(max = 200)
        @Schema(description = "V2 매핑 이름", example = "공용부품 BOM V2 매핑")
        String name,
        @Schema(description = "Excel 시트명")
        String sheetName,
        @Valid
        @NotNull
        @Schema(description = "확정할 V2 매핑")
        MappingV2ResultDto mapping
) {
}
