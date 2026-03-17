package com.fabbitinc.server.presentation.settings.controller;

import com.fabbitinc.server.application.settings.query.SettingsQuery;
import com.fabbitinc.server.application.settings.query.result.SettingsResult;
import com.fabbitinc.server.application.settings.usecase.UpdateSettingsPartWorkflowPolicyUseCase;
import com.fabbitinc.server.application.settings.usecase.command.UpdateSettingsPartWorkflowPolicyCommand;
import com.fabbitinc.server.application.settings.usecase.result.UpdateSettingsPartWorkflowPolicyResult;
import com.fabbitinc.server.presentation.settings.request.SettingsPartWorkflowPolicyRequest;
import com.fabbitinc.server.presentation.settings.response.SettingsPartWorkflowPolicyResponse;
import com.fabbitinc.server.presentation.settings.response.SettingsResponse;
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
@RequestMapping("/api/v1/settings")
@Tag(name = "settings", description = "설정 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class SettingsController {

    private final SettingsQuery settingsQuery;
    private final UpdateSettingsPartWorkflowPolicyUseCase updateSettingsPartWorkflowPolicyUseCase;

    @Operation(summary = "초기 렌더에 필요한 설정 값을 조회합니다", description = "초기 렌더에 필요한 설정 값을 조회합니다")
    @GetMapping
    public SettingsResponse getSettings() {
        SettingsResult result = settingsQuery.get();
        return new SettingsResponse(result.partWorkflowMode());
    }

    @Operation(summary = "부품 워크플로 정책을 변경합니다", description = "부품 워크플로 정책을 변경합니다")
    @PutMapping("/parts/workflow-policy")
    public SettingsPartWorkflowPolicyResponse updatePartWorkflowPolicy(
            @Parameter(description = "부품 워크플로 정책 변경 요청")
            @Valid @RequestBody SettingsPartWorkflowPolicyRequest request
    ) {
        UpdateSettingsPartWorkflowPolicyResult result = updateSettingsPartWorkflowPolicyUseCase.execute(
                new UpdateSettingsPartWorkflowPolicyCommand(request.mode())
        );
        return new SettingsPartWorkflowPolicyResponse(result.mode());
    }
}
