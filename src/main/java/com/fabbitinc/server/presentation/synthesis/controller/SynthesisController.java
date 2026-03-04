package com.fabbitinc.server.presentation.synthesis.controller;

import com.fabbitinc.server.application.synthesis.dto.request.SynthesisStartRequest;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStartResponse;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisBatchStatusResponse;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisJobResponse;
import com.fabbitinc.server.application.synthesis.dto.response.SynthesisListResponse;
import com.fabbitinc.server.application.synthesis.query.SynthesisQuery;
import com.fabbitinc.server.application.synthesis.usecase.StartSynthesisUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/synthesis")
@Tag(name = "synthesis", description = "합성 작업 API")
public class SynthesisController {

    private final StartSynthesisUseCase startSynthesisUseCase;
    private final SynthesisQuery synthesisQuery;

    @Operation(
            summary = "POST /api/v1/synthesis",
            description = "매핑 기반 합성 배치를 시작하고 batch/job 정보를 반환합니다"
    )
    @PostMapping
    public SynthesisBatchStartResponse startSynthesis(
            @Valid @RequestBody SynthesisStartRequest request
    ) {
        return startSynthesisUseCase.execute(request);
    }

    @Operation(
            summary = "GET /api/v1/synthesis/batches/{batchId}",
            description = "합성 배치 진행 상태를 조회합니다"
    )
    @GetMapping("/batches/{batchId}")
    public SynthesisBatchStatusResponse getSynthesisBatch(@PathVariable UUID batchId) {
        return synthesisQuery.getSynthesisBatch(batchId);
    }

    @Operation(
            summary = "GET /api/v1/synthesis/{jobId}",
            description = "개별 합성 작업 상태를 조회합니다"
    )
    @GetMapping("/{jobId}")
    public SynthesisJobResponse getSynthesisJob(@PathVariable UUID jobId) {
        return synthesisQuery.getSynthesisJob(jobId);
    }

    @Operation(
            summary = "GET /api/v1/synthesis",
            description = "전체 합성 작업 이력을 최신순으로 조회합니다"
    )
    @GetMapping
    public SynthesisListResponse listSynthesisJobs() {
        return synthesisQuery.listSynthesisJobs();
    }
}
