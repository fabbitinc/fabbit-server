package com.fabbitinc.server.presentation.engineeringchange.controller;

import com.fabbitinc.server.application.engineeringchange.query.ChangeStatisticsQuery;
import com.fabbitinc.server.application.engineeringchange.query.condition.ChangeStatisticsCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.StepProgressCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeStatisticsResult;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.ChangeStatisticsResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.ChangeStatisticsResponse.TopChangedPartResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.StepProgressResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/change-statistics")
@Tag(name = "change-statistics", description = "변경 통계 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class ChangeStatisticsController {

    private final ChangeStatisticsQuery changeStatisticsQuery;

    @Operation(
            operationId = "changeStatisticsGet",
            summary = "변경 통계를 조회합니다",
            description = "조직 전체의 엔지니어링 변경 통계를 조회합니다. 전체 릴리즈 건수, 이번 달 릴리즈 건수, 평균 승인 소요일, 변경 빈도 상위 파트를 포함합니다."
    )
    @GetMapping
    public ChangeStatisticsResponse get() {
        ChangeStatisticsResult result = changeStatisticsQuery.getStatistics(new ChangeStatisticsCondition());

        return new ChangeStatisticsResponse(
                result.totalReleasedCount(),
                result.monthlyReleasedCount(),
                result.averageApprovalDaysOrNull(),
                result.topChangedParts().stream()
                        .map(part -> new TopChangedPartResponse(
                                part.partId(),
                                part.partNumber(),
                                part.partName(),
                                part.changeCount()
                        ))
                        .toList()
        );
    }

    @Operation(
            operationId = "changeStatisticsStepProgressList",
            summary = "EC별 단계 진행 현황을 조회합니다",
            description = "비종료 상태(RELEASED, CANCELED 제외)의 EC에 대해 스테이지별 진행 현황을 목록으로 조회합니다. 전체 스테이지 수, 완료 스테이지 수, 현재 스테이지의 스텝 상태 집계를 포함합니다."
    )
    @GetMapping("/step-progress")
    public List<StepProgressResponse> listStepProgress() {
        return changeStatisticsQuery.listStepProgress(new StepProgressCondition()).stream()
                .map(r -> new StepProgressResponse(
                        r.engineeringChangeId(),
                        r.totalStages(),
                        r.completedStages(),
                        r.currentStageType(),
                        r.currentStageStepsTotal(),
                        r.currentStageStepsApproved(),
                        r.currentStageStepsPending(),
                        r.currentStageStepsChangesRequested()
                ))
                .toList();
    }
}
