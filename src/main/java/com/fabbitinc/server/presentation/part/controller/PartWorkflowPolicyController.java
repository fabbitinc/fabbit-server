package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartWorkflowPolicyResponse;

import com.fabbitinc.server.application.part.dto.request.PartWorkflowPolicyRequest;
import com.fabbitinc.server.application.part.dto.response.PartWorkflowPolicyResponse;
import com.fabbitinc.server.application.part.query.PartWorkflowPolicyQuery;
import com.fabbitinc.server.application.part.query.result.PartWorkflowPolicyResult;
import com.fabbitinc.server.application.part.usecase.UpdatePartWorkflowPolicyUseCase;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartWorkflowPolicyCommand;
import com.fabbitinc.server.application.part.usecase.result.UpdatePartWorkflowPolicyResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts/workflow-policy")
@Tag(name = "part-workflow-policy", description = "부품 리비전 워크플로 정책 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartWorkflowPolicyController {

    private final PartWorkflowPolicyQuery partWorkflowPolicyQuery;
    private final UpdatePartWorkflowPolicyUseCase updatePartWorkflowPolicyUseCase;

    @Operation(summary = "GET /api/v1/parts/workflow-policy", description = "부품 리비전 워크플로 정책을 조회합니다")
    @GetMapping
    public PartWorkflowPolicyResponse getWorkflowPolicy() {
        PartWorkflowPolicyResult result = partWorkflowPolicyQuery.get();
        return toPartWorkflowPolicyResponse(result);
    }

    @Operation(summary = "PUT /api/v1/parts/workflow-policy", description = "부품 리비전 워크플로 정책을 변경합니다")
    @PutMapping
    public PartWorkflowPolicyResponse updateWorkflowPolicy(
            @Parameter(description = "부품 리비전 워크플로 정책 변경 요청")
            @Valid @RequestBody PartWorkflowPolicyRequest request
    ) {
        UpdatePartWorkflowPolicyResult result = updatePartWorkflowPolicyUseCase.execute(
                new UpdatePartWorkflowPolicyCommand(request.mode())
        );
        return new PartWorkflowPolicyResponse(result.mode());
    }
}
