package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "EC 단계별 진행 현황 응답")
public record StepProgressResponse(
        @Schema(description = "EC 식별자") UUID engineeringChangeId,
        @Schema(description = "전체 스테이지 수", example = "3") int totalStages,
        @Schema(description = "완료된 스테이지 수", example = "1") int completedStages,
        @Schema(description = "현재 스테이지 타입 (REVIEW/APPROVAL/RELEASE), 모든 스테이지 완료 시 null", example = "APPROVAL") String currentStageType,
        @Schema(description = "현재 스테이지의 전체 스텝 수", example = "3") int currentStageStepsTotal,
        @Schema(description = "현재 스테이지의 승인된 스텝 수", example = "1") int currentStageStepsApproved,
        @Schema(description = "현재 스테이지의 대기 중인 스텝 수", example = "1") int currentStageStepsPending,
        @Schema(description = "현재 스테이지의 수정 요청된 스텝 수", example = "1") int currentStageStepsChangesRequested
) {
}
