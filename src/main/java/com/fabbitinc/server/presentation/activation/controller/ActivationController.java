package com.fabbitinc.server.presentation.activation.controller;

import com.fabbitinc.server.application.activation.dto.request.QueryRequest;
import com.fabbitinc.server.application.activation.dto.response.HealthCheckResponse;
import com.fabbitinc.server.application.activation.dto.response.QueryResponse;
import com.fabbitinc.server.application.activation.dto.response.StartersResponse;
import com.fabbitinc.server.application.activation.query.ActivationQuery;
import com.fabbitinc.server.application.activation.usecase.HealthCheckUseCase;
import com.fabbitinc.server.application.activation.usecase.QueryGraphUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activation")
@Tag(name = "activation", description = "활성화 및 탐색 API")
public class ActivationController {

    private final HealthCheckUseCase healthCheckUseCase;
    private final QueryGraphUseCase queryGraphUseCase;
    private final ActivationQuery activationQuery;

    @Operation(
            summary = "POST /api/v1/activation/health-check",
            description = "그래프/관계 데이터 상태를 점검하여 이슈를 반환합니다"
    )
    @PostMapping("/health-check")
    public HealthCheckResponse healthCheck(
) {
        return healthCheckUseCase.execute();
    }

    @Operation(
            summary = "POST /api/v1/activation/query",
            description = "자연어 질문을 실행해 탐색 결과와 요약 답변을 반환합니다"
    )
    @PostMapping("/query")
    public QueryResponse queryGraph(
            @Valid @RequestBody QueryRequest request
    ) {
        return queryGraphUseCase.execute(request.question());
    }

    @Operation(
            summary = "GET /api/v1/activation/starters",
            description = "초기 탐색용 추천 질문 목록을 조회합니다"
    )
    @GetMapping("/starters")
    public StartersResponse getStarters(
) {
        return activationQuery.getStarters();
    }
}
