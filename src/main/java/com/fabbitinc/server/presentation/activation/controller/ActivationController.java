package com.fabbitinc.server.presentation.activation.controller;

import com.fabbitinc.server.application.activation.dto.request.QueryRequest;
import com.fabbitinc.server.application.activation.dto.response.ActivationResultType;
import com.fabbitinc.server.application.activation.dto.response.HealthCheckIssueResponse;
import com.fabbitinc.server.application.activation.dto.response.HealthCheckResponse;
import com.fabbitinc.server.application.activation.dto.response.HealthIssueCategory;
import com.fabbitinc.server.application.activation.dto.response.HealthIssueSeverity;
import com.fabbitinc.server.application.activation.dto.response.QueryResponse;
import com.fabbitinc.server.application.activation.dto.response.QueryResultResponse;
import com.fabbitinc.server.application.activation.dto.response.StarterQuestionResponse;
import com.fabbitinc.server.application.activation.dto.response.StartersResponse;
import com.fabbitinc.server.application.activation.query.ActivationQuery;
import com.fabbitinc.server.application.activation.query.condition.ActivationStartersCondition;
import com.fabbitinc.server.application.activation.query.result.ActivationStarterQuestionResult;
import com.fabbitinc.server.application.activation.query.result.ActivationStartersResult;
import com.fabbitinc.server.application.activation.usecase.HealthCheckUseCase;
import com.fabbitinc.server.application.activation.usecase.QueryGraphUseCase;
import com.fabbitinc.server.application.activation.usecase.command.QueryGraphCommand;
import com.fabbitinc.server.application.activation.usecase.result.HealthCheckIssueResult;
import com.fabbitinc.server.application.activation.usecase.result.HealthCheckResult;
import com.fabbitinc.server.application.activation.usecase.result.QueryGraphItemResult;
import com.fabbitinc.server.application.activation.usecase.result.QueryGraphResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "점검 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/health-check")
    public HealthCheckResponse healthCheck(
) {
        return toHealthCheckResponse(healthCheckUseCase.execute());
    }

    @Operation(
            summary = "POST /api/v1/activation/query",
            description = "자연어 질문을 실행해 탐색 결과와 요약 답변을 반환합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질의 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/query")
    public QueryResponse queryGraph(
            @Parameter(description = "그래프 자연어 질의 요청")
            @Valid @RequestBody QueryRequest request
    ) {
        return toQueryResponse(queryGraphUseCase.execute(new QueryGraphCommand(request.question())));
    }

    @Operation(
            summary = "GET /api/v1/activation/starters",
            description = "초기 탐색용 추천 질문 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/starters")
    public StartersResponse getStarters(
) {
        return toStartersResponse(activationQuery.lookup(new ActivationStartersCondition()));
    }

    private StartersResponse toStartersResponse(ActivationStartersResult result) {
        return new StartersResponse(
                result.starters().stream()
                        .map(this::toStarterQuestionResponse)
                        .toList()
        );
    }

    private StarterQuestionResponse toStarterQuestionResponse(ActivationStarterQuestionResult result) {
        return new StarterQuestionResponse(
                result.question(),
                result.description()
        );
    }

    private HealthCheckResponse toHealthCheckResponse(HealthCheckResult result) {
        return new HealthCheckResponse(
                result.totalNodes(),
                result.totalRelationships(),
                result.nodeCounts(),
                result.relationshipCounts(),
                result.issues().stream()
                        .map(this::toHealthCheckIssueResponse)
                        .toList()
        );
    }

    private HealthCheckIssueResponse toHealthCheckIssueResponse(HealthCheckIssueResult result) {
        return new HealthCheckIssueResponse(
                HealthIssueCategory.from(result.category()),
                HealthIssueSeverity.from(result.severity()),
                result.message(),
                result.count()
        );
    }

    private QueryResponse toQueryResponse(QueryGraphResult result) {
        return new QueryResponse(
                result.results().stream()
                        .map(this::toQueryResultResponse)
                        .toList(),
                result.answer()
        );
    }

    private QueryResultResponse toQueryResultResponse(QueryGraphItemResult result) {
        return new QueryResultResponse(
                ActivationResultType.from(result.type()),
                result.key(),
                result.label(),
                result.description(),
                result.value()
        );
    }
}
