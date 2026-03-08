package com.fabbitinc.server.presentation.drawing.controller;

import com.fabbitinc.server.application.drawing.dto.response.DrawingProcessingResponse;
import com.fabbitinc.server.application.drawing.query.DrawingProcessingQuery;
import com.fabbitinc.server.application.drawing.query.condition.DrawingProcessingCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/drawings")
@Tag(name = "drawings", description = "도면 처리 상태 조회 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class DrawingController {

    private final DrawingProcessingQuery drawingProcessingQuery;

    @Operation(
            summary = "GET /api/v1/drawings/{drawingId}/processing",
            description = "도면 비동기 처리 상태와 산출물 준비 여부를 조회합니다"
    )
    @GetMapping("/{drawingId}/processing")
    public DrawingProcessingResponse getProcessing(
            @Parameter(description = "조회할 도면 ID")
            @PathVariable UUID drawingId
    ) {
        return toDrawingProcessingResponse(drawingProcessingQuery.get(new DrawingProcessingCondition(drawingId)));
    }

    private DrawingProcessingResponse toDrawingProcessingResponse(
            com.fabbitinc.server.application.drawing.query.result.DrawingProcessingResult result
    ) {
        return new DrawingProcessingResponse(
                result.status(),
                result.failureReason(),
                result.pdfReady(),
                result.webpReady(),
                result.glbReady()
        );
    }
}
