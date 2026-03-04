package com.fabbitinc.server.presentation.dashboard.controller;

import com.fabbitinc.server.application.dashboard.dto.response.DashboardStatsResponse;
import com.fabbitinc.server.application.dashboard.query.DashboardQuery;
import io.swagger.v3.oas.annotations.Operation;
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
    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        return dashboardQuery.getStats();
    }
}
