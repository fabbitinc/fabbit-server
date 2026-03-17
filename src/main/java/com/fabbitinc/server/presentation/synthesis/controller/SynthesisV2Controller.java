package com.fabbitinc.server.presentation.synthesis.controller;

import com.fabbitinc.server.presentation.synthesis.dto.request.SynthesisStartRequest;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisBatchFailure;
import com.fabbitinc.server.presentation.synthesis.dto.response.SynthesisBatchStartResponse;
import com.fabbitinc.server.application.synthesisv2.query.SynthesisV2Query;
import com.fabbitinc.server.application.synthesisv2.query.condition.SynthesisV2BatchCondition;
import com.fabbitinc.server.application.synthesisv2.query.condition.SynthesisV2JobCondition;
import com.fabbitinc.server.application.synthesisv2.query.condition.SynthesisV2ListCondition;
import com.fabbitinc.server.application.synthesisv2.query.result.SynthesisV2BatchStatusResult;
import com.fabbitinc.server.application.synthesisv2.query.result.SynthesisV2JobResult;
import com.fabbitinc.server.application.synthesisv2.query.result.SynthesisV2ListResult;
import com.fabbitinc.server.application.synthesisv2.usecase.StartSynthesisV2UseCase;
import com.fabbitinc.server.application.synthesisv2.usecase.command.StartSynthesisV2Command;
import com.fabbitinc.server.application.synthesisv2.usecase.command.StartSynthesisV2UploadCommand;
import com.fabbitinc.server.application.synthesisv2.usecase.result.StartSynthesisV2FailureResult;
import com.fabbitinc.server.application.synthesisv2.usecase.result.StartedSynthesisV2BatchResult;
import com.fabbitinc.server.application.synthesisv2.usecase.result.StartedSynthesisV2JobResult;
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
@RequestMapping("/api/v2/synthesis")
@Tag(name = "synthesis-v2", description = "V2 합성 작업 API")
public class SynthesisV2Controller {

    private final StartSynthesisV2UseCase startSynthesisV2UseCase;
    private final SynthesisV2Query synthesisV2Query;

    @Operation(
            summary = "V2 매핑 기반 합성 배치를 시작하고 batch/job 정보를 반환합니다",
            description = "V2 매핑 기반 합성 배치를 시작하고 batch/job 정보를 반환합니다"
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
            @Parameter(description = "V2 합성 시작 요청")
            @Valid @RequestBody SynthesisStartRequest request
    ) {
        return toSynthesisBatchStartResponse(startSynthesisV2UseCase.execute(
                new StartSynthesisV2Command(
                        request.mappingId(),
                        request.projectId(),
                        request.overwrite(),
                        request.uploads().stream()
                                .map(item -> new StartSynthesisV2UploadCommand(item.fileId(), item.rootContext()))
                                .toList()
                )
        ));
    }

    @Operation(
            summary = "V2 합성 배치 진행 상태를 조회합니다",
            description = "V2 합성 배치 진행 상태를 조회합니다"
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
            @Parameter(description = "조회할 V2 합성 배치 ID") @PathVariable UUID batchId
    ) {
        return toSynthesisBatchStatusResponse(synthesisV2Query.getBatch(new SynthesisV2BatchCondition(batchId)));
    }

    @Operation(
            summary = "개별 V2 합성 작업 상태를 조회합니다",
            description = "개별 V2 합성 작업 상태를 조회합니다"
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
            @Parameter(description = "조회할 V2 합성 작업 ID") @PathVariable UUID jobId
    ) {
        return toSynthesisJobResponse(synthesisV2Query.getJob(new SynthesisV2JobCondition(jobId)));
    }

    @Operation(
            summary = "전체 V2 합성 작업 이력을 최신순으로 조회합니다",
            description = "전체 V2 합성 작업 이력을 최신순으로 조회합니다"
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
        return toSynthesisListResponse(synthesisV2Query.list(new SynthesisV2ListCondition()));
    }

    private SynthesisBatchStatusResponse toSynthesisBatchStatusResponse(SynthesisV2BatchStatusResult result) {
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

    private SynthesisBatchStartResponse toSynthesisBatchStartResponse(StartedSynthesisV2BatchResult result) {
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
            StartedSynthesisV2JobResult result
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

    private SynthesisBatchFailure toSynthesisBatchFailure(StartSynthesisV2FailureResult result) {
        return new SynthesisBatchFailure(result.fileId(), result.reason());
    }

    private SynthesisJobResponse toSynthesisJobResponse(SynthesisV2JobResult result) {
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

    private SynthesisListResponse toSynthesisListResponse(SynthesisV2ListResult result) {
        return new SynthesisListResponse(
                result.items().stream()
                        .map(this::toSynthesisJobResponse)
                        .toList()
        );
    }
}
