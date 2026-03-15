package com.fabbitinc.server.presentation.mapping.controller;

import com.fabbitinc.server.presentation.mapping.dto.request.MappingV2ConfirmRequest;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingV2PreviewRequest;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingV2UpdateRequest;
import com.fabbitinc.server.presentation.mapping.dto.request.MappingV2ValidateRequest;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingImpactSummaryResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingV2ListResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingV2PreviewResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingV2Response;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingV2ValidateResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.SheetPreviewV2Response;
import com.fabbitinc.server.presentation.mapping.dto.response.SkippedSheetResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.ValidationIssueResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.ValidationSeverity;
import com.fabbitinc.server.application.mappingv2.query.MappingV2Query;
import com.fabbitinc.server.application.mappingv2.query.condition.MappingV2DetailCondition;
import com.fabbitinc.server.application.mappingv2.query.condition.MappingV2ListCondition;
import com.fabbitinc.server.application.mappingv2.query.result.MappingV2ListResult;
import com.fabbitinc.server.application.mappingv2.query.result.MappingV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.ConfirmMappingV2UseCase;
import com.fabbitinc.server.application.mappingv2.usecase.DeactivateMappingV2UseCase;
import com.fabbitinc.server.application.mappingv2.usecase.PreviewMappingV2UseCase;
import com.fabbitinc.server.application.mappingv2.usecase.UpdateMappingV2UseCase;
import com.fabbitinc.server.application.mappingv2.usecase.ValidateMappingV2UseCase;
import com.fabbitinc.server.application.mappingv2.usecase.command.ConfirmMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.command.DeactivateMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.command.PreviewMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.command.UpdateMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.command.ValidateMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.result.PreviewMappingV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.result.PreviewSheetV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.result.SavedMappingV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.result.ValidatedMappingV2Result;
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
@RequestMapping("/api/v2/mappings")
@Tag(name = "mappings-v2", description = "V2 매핑 생성/검증 API")
public class MappingV2Controller {

    private final ConfirmMappingV2UseCase confirmMappingV2UseCase;
    private final PreviewMappingV2UseCase previewMappingV2UseCase;
    private final ValidateMappingV2UseCase validateMappingV2UseCase;
    private final UpdateMappingV2UseCase updateMappingV2UseCase;
    private final DeactivateMappingV2UseCase deactivateMappingV2UseCase;
    private final MappingV2Query mappingV2Query;

    @Operation(
            summary = "POST /api/v2/mappings/preview",
            description = "업로드된 파일을 nodes[] + relations[] 구조의 V2 매핑으로 미리보기합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "미리보기 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/preview")
    public MappingV2PreviewResponse preview(
            @Parameter(description = "V2 매핑 미리보기 요청")
            @Valid @RequestBody MappingV2PreviewRequest request
    ) {
        return toPreviewResponse(previewMappingV2UseCase.execute(
                new PreviewMappingV2Command(request.fileId(), request.sheetName())
        ));
    }

    @Operation(
            summary = "POST /api/v2/mappings/confirm",
            description = "검토된 V2 매핑을 확정하여 새 V2 매핑 레코드(버전 1)를 생성합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매핑 확정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/confirm")
    public MappingV2Response confirm(
            @Parameter(description = "V2 매핑 확정 요청")
            @Valid @RequestBody MappingV2ConfirmRequest request
    ) {
        return toMappingResponse(confirmMappingV2UseCase.execute(new ConfirmMappingV2Command(
                request.fileId(),
                request.name(),
                request.sheetName(),
                request.mapping()
        )));
    }

    @Operation(
            summary = "POST /api/v2/mappings/validate",
            description = "V2 매핑을 정규화하고 파일 샘플 데이터 기준으로 오류/경고를 검증합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PostMapping("/validate")
    public MappingV2ValidateResponse validate(
            @Parameter(description = "V2 매핑 검증 요청")
            @Valid @RequestBody MappingV2ValidateRequest request
    ) {
        return toValidateResponse(validateMappingV2UseCase.execute(
                new ValidateMappingV2Command(request.fileId(), request.sheetName(), request.mapping())
        ));
    }

    @Operation(
            summary = "GET /api/v2/mappings",
            description = "활성 V2 매핑 목록을 최신순으로 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public MappingV2ListResponse list() {
        return toMappingListResponse(mappingV2Query.list(new MappingV2ListCondition()));
    }

    @Operation(
            summary = "GET /api/v2/mappings/{mappingId}",
            description = "V2 매핑 ID로 최신 리비전을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/{mappingId}")
    public MappingV2Response get(
            @Parameter(description = "조회할 V2 매핑 ID") @PathVariable java.util.UUID mappingId
    ) {
        return toMappingResponse(mappingV2Query.get(new MappingV2DetailCondition(mappingId)));
    }

    @Operation(
            summary = "PUT /api/v2/mappings/{mappingId}",
            description = "V2 매핑을 수정하고 새로운 리비전을 생성합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @PutMapping("/{mappingId}")
    public MappingV2Response update(
            @Parameter(description = "수정할 V2 매핑 ID") @PathVariable java.util.UUID mappingId,
            @Parameter(description = "V2 매핑 수정 요청")
            @Valid @RequestBody MappingV2UpdateRequest request
    ) {
        return toMappingResponse(updateMappingV2UseCase.execute(new UpdateMappingV2Command(
                mappingId,
                request.fileId(),
                request.name(),
                request.sheetName(),
                request.mapping()
        )));
    }

    @Operation(
            summary = "DELETE /api/v2/mappings/{mappingId}",
            description = "V2 매핑을 비활성화(soft delete)합니다"
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
            @Parameter(description = "비활성화할 V2 매핑 ID") @PathVariable java.util.UUID mappingId
    ) {
        deactivateMappingV2UseCase.execute(new DeactivateMappingV2Command(mappingId));
        return ResponseEntity.noContent().build();
    }

    private MappingV2PreviewResponse toPreviewResponse(PreviewMappingV2Result result) {
        return new MappingV2PreviewResponse(
                result.headers(),
                result.sampleRows(),
                result.mapping(),
                result.sheets().stream()
                        .map(this::toSheetPreviewResponse)
                        .toList(),
                result.skippedSheets().stream()
                        .map(skipped -> new SkippedSheetResponse(skipped.sheetName(), skipped.reason()))
                        .toList()
        );
    }

    private SheetPreviewV2Response toSheetPreviewResponse(PreviewSheetV2Result result) {
        return new SheetPreviewV2Response(
                result.sheetName(),
                result.headers(),
                result.sampleRows(),
                result.mapping()
        );
    }

    private MappingV2ValidateResponse toValidateResponse(ValidatedMappingV2Result result) {
        return new MappingV2ValidateResponse(
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

    private MappingV2Response toMappingResponse(SavedMappingV2Result result) {
        return new MappingV2Response(
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

    private MappingV2Response toMappingResponse(MappingV2Result result) {
        return new MappingV2Response(
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

    private MappingV2ListResponse toMappingListResponse(MappingV2ListResult result) {
        return new MappingV2ListResponse(result.items().stream()
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
