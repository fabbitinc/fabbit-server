package com.fabbitinc.server.presentation.dashboard.controller;

import com.fabbitinc.server.presentation.dashboard.dto.response.BomStatsResponse;
import com.fabbitinc.server.presentation.dashboard.dto.response.DashboardStatsResponse;
import com.fabbitinc.server.presentation.dashboard.dto.response.LastSynthesisResponse;
import com.fabbitinc.server.presentation.dashboard.dto.response.PartStatsResponse;
import com.fabbitinc.server.application.dashboard.query.DashboardQuery;
import com.fabbitinc.server.application.dashboard.query.condition.DashboardStatsCondition;
import com.fabbitinc.server.application.dashboard.query.result.DashboardLastSynthesisResult;
import com.fabbitinc.server.application.dashboard.query.result.DashboardStatsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@Tag(name = "dashboard", description = "대시보드 통계 API")
public class DashboardController {

    private final DashboardQuery dashboardQuery;

    @Operation(
            summary = "GET /api/v1/dashboard/stats",
            description = "Part 총 수, 금주 추가 수, BOM 링크 수, 최근 합성 작업 상태를 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return toDashboardStatsResponse(dashboardQuery.get(new DashboardStatsCondition()));
    }

    private DashboardStatsResponse toDashboardStatsResponse(DashboardStatsResult result) {
        return new DashboardStatsResponse(
                new PartStatsResponse(
                        result.parts().total(),
                        result.parts().addedThisWeek()
                ),
                new BomStatsResponse(result.bomLinks().total()),
                toLastSynthesisResponse(result.lastSynthesis())
        );
    }

    private LastSynthesisResponse toLastSynthesisResponse(DashboardLastSynthesisResult result) {
        if (result == null) {
            return null;
        }
        return new LastSynthesisResponse(
                result.jobId(),
                result.status(),
                result.completedAt(),
                result.nodesCreated(),
                result.relationshipsCreated()
        );
    }
}
