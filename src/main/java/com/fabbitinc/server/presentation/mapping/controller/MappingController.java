package com.fabbitinc.server.presentation.mapping.controller;

import com.fabbitinc.server.application.mapping.query.MappingQuery;
import com.fabbitinc.server.application.mapping.query.condition.MappingDetailCondition;
import com.fabbitinc.server.application.mapping.query.condition.MappingListCondition;
import com.fabbitinc.server.application.mapping.query.result.MappingListResult;
import com.fabbitinc.server.application.mapping.query.result.MappingResult;
import com.fabbitinc.server.application.mapping.usecase.ConfirmMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.DeactivateMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.PreviewMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.UpdateMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.ValidateMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.command.ConfirmMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.command.DeactivateMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.command.PreviewMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.command.UpdateMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.command.ValidateMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewMappingResult;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewSheetResult;
import com.fabbitinc.server.application.mapping.usecase.result.SavedMappingResult;
import com.fabbitinc.server.application.mapping.usecase.result.ValidatedMappingResult;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingConfirmRequest;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingPreviewRequest;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingUpdateRequest;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingValidateRequest;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingImpactSummaryResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingListResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingPreviewResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingValidateResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.SheetPreviewResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.SkippedSheetResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.ValidationIssueResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.ValidationSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mappings")
@Tag(name = "mappings", description = "매핑 생성/검증 API")
public class MappingController {

    private final ConfirmMappingUseCase confirmMappingUseCase;
    private final PreviewMappingUseCase previewMappingUseCase;
    private final ValidateMappingUseCase validateMappingUseCase;
    private final UpdateMappingUseCase updateMappingUseCase;
    private final DeactivateMappingUseCase deactivateMappingUseCase;
    private final MappingQuery mappingQuery;

    @Operation(
            operationId = "mappingPreview",
            summary = "POST /api/v1/mappings/preview",
            description = "업로드된 파일을 nodes[] + relations[] 구조의 매핑으로 미리보기합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "미리보기 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/preview")
    public MappingPreviewResponse preview(
            @Parameter(description = "매핑 미리보기 요청")
            @Valid @RequestBody MappingPreviewRequest request
    ) {
        return toPreviewResponse(previewMappingUseCase.execute(
                new PreviewMappingCommand(request.fileId(), request.sheetName())
        ));
    }

    @Operation(
            operationId = "mappingConfirm",
            summary = "검토된 매핑을 확정하여 새 매핑 레코드(버전 1)를 생성합니다",
            description = "검토된 매핑을 확정하여 새 매핑 레코드(버전 1)를 생성합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매핑 확정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/confirm")
    public MappingResponse confirm(
            @Parameter(description = "매핑 확정 요청")
            @Valid @RequestBody MappingConfirmRequest request
    ) {
        return toMappingResponse(confirmMappingUseCase.execute(new ConfirmMappingCommand(
                request.fileId(),
                request.name(),
                request.sheetName(),
                request.mapping()
        )));
    }

    @Operation(
            operationId = "mappingValidate",
            summary = "매핑을 정규화하고 파일 샘플 데이터 기준으로 오류/경고를 검증합니다",
            description = "매핑을 정규화하고 파일 샘플 데이터 기준으로 오류/경고를 검증합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/validate")
    public MappingValidateResponse validate(
            @Parameter(description = "매핑 검증 요청")
            @Valid @RequestBody MappingValidateRequest request
    ) {
        return toValidateResponse(validateMappingUseCase.execute(
                new ValidateMappingCommand(request.fileId(), request.sheetName(), request.mapping())
        ));
    }

    @Operation(
            operationId = "mappingList",
            summary = "활성 매핑 목록을 최신순으로 조회합니다",
            description = "활성 매핑 목록을 최신순으로 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public MappingListResponse list() {
        return toMappingListResponse(mappingQuery.list(new MappingListCondition()));
    }

    @Operation(
            operationId = "mappingGet",
            summary = "매핑 ID로 최신 리비전을 조회합니다",
            description = "매핑 ID로 최신 리비전을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/{mappingId}")
    public MappingResponse get(
            @Parameter(description = "조회할 매핑 ID") @PathVariable java.util.UUID mappingId
    ) {
        return toMappingResponse(mappingQuery.get(new MappingDetailCondition(mappingId)));
    }

    @Operation(
            operationId = "mappingUpdate",
            summary = "매핑을 수정하고 새로운 리비전을 생성합니다",
            description = "매핑을 수정하고 새로운 리비전을 생성합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PutMapping("/{mappingId}")
    public MappingResponse update(
            @Parameter(description = "수정할 매핑 ID") @PathVariable java.util.UUID mappingId,
            @Parameter(description = "매핑 수정 요청")
            @Valid @RequestBody MappingUpdateRequest request
    ) {
        return toMappingResponse(updateMappingUseCase.execute(new UpdateMappingCommand(
                mappingId,
                request.fileId(),
                request.name(),
                request.sheetName(),
                request.mapping()
        )));
    }

    @Operation(
            operationId = "mappingDelete",
            summary = "매핑을 비활성화(soft delete)합니다",
            description = "매핑을 비활성화(soft delete)합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "비활성화할 매핑 ID") @PathVariable java.util.UUID mappingId
    ) {
        deactivateMappingUseCase.execute(new DeactivateMappingCommand(mappingId));
        return ResponseEntity.noContent().build();
    }

    private MappingPreviewResponse toPreviewResponse(PreviewMappingResult result) {
        return new MappingPreviewResponse(
                result.headers(),
                result.sampleRows(),
                result.mapping(),
                result.sheets().stream()
                        .map(this::toSheetPreviewResponse)
                        .toList(),
                result.skippedSheets().stream()
                        .map(s -> new SkippedSheetResponse(s.sheetName(), s.reason()))
                        .toList()
        );
    }

    private SheetPreviewResponse toSheetPreviewResponse(PreviewSheetResult result) {
        return new SheetPreviewResponse(
                result.sheetName(),
                result.headers(),
                result.sampleRows(),
                result.mapping()
        );
    }

    private MappingValidateResponse toValidateResponse(ValidatedMappingResult result) {
        return new MappingValidateResponse(
                result.normalizedMapping(),
                result.errors().stream()
                        .map(issue -> new ValidationIssueResponse(
                                issue.code(),
                                toValidationSeverity(issue.severity()),
                                issue.message(),
                                issue.path(),
                                issue.dismissedReason()
                        ))
                        .toList(),
                result.warnings().stream()
                        .map(issue -> new ValidationIssueResponse(
                                issue.code(),
                                toValidationSeverity(issue.severity()),
                                issue.message(),
                                issue.path(),
                                issue.dismissedReason()
                        ))
                        .toList(),
                new MappingImpactSummaryResponse(result.impactSummary().disabledColumnCount())
        );
    }

    private MappingResponse toMappingResponse(SavedMappingResult result) {
        return new MappingResponse(
                result.id(),
                result.fileId(),
                result.name(),
                result.sheetName(),
                result.originalHeaders(),
                result.mappedHeaders(),
                result.mapping(),
                result.active(),
                result.usageCount(),
                result.version(),
                result.createdAt()
        );
    }

    private MappingResponse toMappingResponse(MappingResult result) {
        return new MappingResponse(
                result.id(),
                result.fileId(),
                result.name(),
                result.sheetName(),
                result.originalHeaders(),
                result.mappedHeaders(),
                result.mapping(),
                result.active(),
                result.usageCount(),
                result.version(),
                result.createdAt()
        );
    }

    private MappingListResponse toMappingListResponse(MappingListResult result) {
        return new MappingListResponse(result.items().stream()
                .map(this::toMappingResponse)
                .toList());
    }

    private ValidationSeverity toValidationSeverity(String severity) {
        if (severity == null) {
            return ValidationSeverity.WARNING;
        }
        return "error".equalsIgnoreCase(severity)
                ? ValidationSeverity.ERROR
                : ValidationSeverity.WARNING;
    }
}
