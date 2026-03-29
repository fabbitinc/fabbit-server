package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경 통계 응답")
public record ChangeStatisticsResponse(
        @Schema(description = "전체 릴리즈된 EC 수", example = "150") int totalReleasedCount,
        @Schema(description = "이번 달 릴리즈된 EC 수", example = "12") int monthlyReleasedCount,
        @Schema(description = "평균 승인 소요일 (DRAFT → RELEASED), 데이터 없으면 null", example = "3.5") Double averageApprovalDaysOrNull,
        @Schema(description = "변경 빈도 상위 5개 파트") List<TopChangedPartResponse> topChangedParts
) {

    @Schema(description = "변경 빈도 상위 파트")
    public record TopChangedPartResponse(
            @Schema(description = "파트 식별자") UUID partId,
            @Schema(description = "파트 번호", example = "PN-001") String partNumber,
            @Schema(description = "파트 이름", example = "브래킷 A") String partName,
            @Schema(description = "변경 횟수", example = "8") int changeCount
    ) {
    }
}
