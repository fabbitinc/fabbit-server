package com.fabbitinc.server.presentation.common.controller;

import com.fabbitinc.server.presentation.common.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "health", description = "애플리케이션 헬스체크 API")
public class HealthController {

    @Operation(summary = "애플리케이션 기본 상태를 확인합니다", description = "애플리케이션 기본 상태를 확인합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정상 응답")
    })
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }
}
