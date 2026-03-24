package com.fabbitinc.server.presentation.synthesis.controller;

import com.fabbitinc.server.application.synthesis.query.SynthesisQuery;
import com.fabbitinc.server.application.synthesis.query.condition.SynthesisBatchCondition;
import com.fabbitinc.server.application.synthesis.query.condition.SynthesisJobCondition;
import com.fabbitinc.server.application.synthesis.query.condition.SynthesisListCondition;
import com.fabbitinc.server.application.synthesis.query.result.SynthesisBatchStatusResult;
import com.fabbitinc.server.application.synthesis.query.result.SynthesisJobResult;
import com.fabbitinc.server.application.synthesis.query.result.SynthesisListResult;
import com.fabbitinc.server.application.synthesis.usecase.StartSynthesisUseCase;
import com.fabbitinc.server.application.synthesis.usecase.command.StartSynthesisCommand;
import com.fabbitinc.server.application.synthesis.usecase.command.StartSynthesisUploadCommand;
import com.fabbitinc.server.application.synthesis.usecase.result.StartSynthesisFailureResult;
import com.fabbitinc.server.application.synthesis.usecase.result.StartedSynthesisBatchResult;
import com.fabbitinc.server.application.synthesis.usecase.result.StartedSynthesisJobResult;
import com.fabbitinc.server.presentation.synthesis.dto.request.SynthesisStartRequest;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisBatchFailure;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisBatchStartResponse;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisBatchStatusResponse;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisJobResponse;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/synthesis")
@Tag(name = "synthesis", description = "합성 작업 API")
public class SynthesisController {

    private final StartSynthesisUseCase startSynthesisUseCase;
    private final SynthesisQuery synthesisQuery;

    @Operation(
            summary = "매핑 기반 합성 배치를 시작하고 batch/job 정보를 반환합니다",
            description = "매핑 기반 합성 배치를 시작하고 batch/job 정보를 반환합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "합성 시작 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping
    public SynthesisBatchStartResponse startSynthesis(
            @Parameter(description = "합성 시작 요청")
            @Valid @RequestBody SynthesisStartRequest request
    ) {
        return toSynthesisBatchStartResponse(startSynthesisUseCase.execute(
                new StartSynthesisCommand(
                        request.mappingId(),
                        request.projectId(),
                        request.overwrite(),
                        request.uploads().stream()
                                .map(item -> new StartSynthesisUploadCommand(item.fileId(), item.rootContext()))
                                .toList()
                )
        ));
    }

    @Operation(
            summary = "합성 배치 진행 상태를 조회합니다",
            description = "합성 배치 진행 상태를 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/batches/{batchId}")
    public SynthesisBatchStatusResponse getSynthesisBatch(
            @Parameter(description = "조회할 합성 배치 ID") @PathVariable UUID batchId
    ) {
        return toSynthesisBatchStatusResponse(synthesisQuery.getBatch(new SynthesisBatchCondition(batchId)));
    }

    @Operation(
            summary = "개별 합성 작업 상태를 조회합니다",
            description = "개별 합성 작업 상태를 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/{jobId}")
    public SynthesisJobResponse getSynthesisJob(
            @Parameter(description = "조회할 합성 작업 ID") @PathVariable UUID jobId
    ) {
        return toSynthesisJobResponse(synthesisQuery.getJob(new SynthesisJobCondition(jobId)));
    }

    @Operation(
            summary = "전체 합성 작업 이력을 최신순으로 조회합니다",
            description = "전체 합성 작업 이력을 최신순으로 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public SynthesisListResponse listSynthesisJobs() {
        return toSynthesisListResponse(synthesisQuery.list(new SynthesisListCondition()));
    }

    private SynthesisBatchStatusResponse toSynthesisBatchStatusResponse(SynthesisBatchStatusResult result) {
        return new SynthesisBatchStatusResponse(
                result.batchId(),
                result.requestedCount(),
                result.acceptedCount(),
                result.failedCount(),
                result.pendingCount(),
                result.processingCount(),
                result.completedCount(),
                result.failedJobCount(),
                SynthesisBatchStatusResponse.Status.valueOf(result.status().name()),
                result.failed().stream()
                        .map(item -> new SynthesisBatchStatusResponse.SynthesisBatchFailureResponse(
                                item.fileId(),
                                item.reason()
                        ))
                        .toList(),
                result.items().stream()
                        .map(item -> new SynthesisBatchStatusResponse.SynthesisBatchItemStatusResponse(
                                item.jobId(),
                                item.fileId(),
                                item.status(),
                                item.totalRows(),
                                item.processedRows(),
                                item.nodesCreated(),
                                item.relationshipsCreated(),
                                item.errorCount(),
                                item.startedAt(),
                                item.completedAt()
                        ))
                        .toList(),
                result.createdAt()
        );
    }

    private SynthesisBatchStartResponse toSynthesisBatchStartResponse(StartedSynthesisBatchResult result) {
        return new SynthesisBatchStartResponse(
                result.batchId(),
                result.requestedCount(),
                result.acceptedCount(),
                result.items().stream()
                        .map(this::toStartedSynthesisJobResponse)
                        .toList(),
                result.failed().stream()
                        .map(this::toSynthesisBatchFailure)
                        .toList()
        );
    }

    private SynthesisJobResponse toStartedSynthesisJobResponse(
            StartedSynthesisJobResult result
    ) {
        return new SynthesisJobResponse(
                result.id(),
                result.mappingId(),
                result.fileId(),
                result.status(),
                result.totalRows(),
                result.processedRows(),
                result.nodesCreated(),
                result.relationshipsCreated(),
                result.errors(),
                result.startedAt(),
                result.completedAt(),
                result.createdAt()
        );
    }

    private SynthesisBatchFailure toSynthesisBatchFailure(StartSynthesisFailureResult result) {
        return new SynthesisBatchFailure(result.fileId(), result.reason());
    }

    private SynthesisJobResponse toSynthesisJobResponse(SynthesisJobResult result) {
        return new SynthesisJobResponse(
                result.id(),
                result.mappingId(),
                result.fileId(),
                result.status(),
                result.totalRows(),
                result.processedRows(),
                result.nodesCreated(),
                result.relationshipsCreated(),
                result.errors(),
                result.startedAt(),
                result.completedAt(),
                result.createdAt()
        );
    }

    private SynthesisListResponse toSynthesisListResponse(SynthesisListResult result) {
        return new SynthesisListResponse(
                result.items().stream()
                        .map(this::toSynthesisJobResponse)
                        .toList()
        );
    }
}
