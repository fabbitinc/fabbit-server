package com.fabbitinc.server.presentation.bom.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "BOM 항목 일괄 추가 요청")
public record AddBomItemsBatchRequest(

        @Schema(description = "BOM 항목 목록")
        @NotNull(message = "항목 목록은 필수입니다") @Size(min = 1, max = 500, message = "항목은 1개 이상 500개 이하여야 합니다") List<@Valid AddBomItemRequest> items
) {
}
