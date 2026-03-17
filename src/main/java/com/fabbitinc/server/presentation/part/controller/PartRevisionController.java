package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toBomTreeResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartBomResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartProjectsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartRevisionDiffResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartRevisionHistoryResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartSuppliersResponse;

import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionDiffCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionHistoryCondition;
import com.fabbitinc.server.application.part.query.condition.PartSuppliersCondition;
import com.fabbitinc.server.application.part.usecase.ReleasePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartRevisionResult;
import com.fabbitinc.server.presentation.common.web.ApiErrorResponse;
import com.fabbitinc.server.presentation.part.request.PartRevisionChangeReasonRequest;
import com.fabbitinc.server.presentation.part.response.BomTreeResponse;
import com.fabbitinc.server.presentation.part.response.PartBomResponse;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import com.fabbitinc.server.presentation.part.response.PartProjectsResponse;
import com.fabbitinc.server.presentation.part.response.PartRevisionDiffResponse;
import com.fabbitinc.server.presentation.part.response.PartRevisionHistoryResponse;
import com.fabbitinc.server.presentation.part.response.PartSuppliersResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-revisions", description = "공식 부품 리비전 조회 및 직접 릴리즈 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartRevisionController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PartQuery partQuery;
    private final ReleasePartRevisionUseCase releasePartRevisionUseCase;

    @Operation(summary = "Part 상세 정보와 관계 카운트를 조회합니다", description = "Part 상세 정보와 관계 카운트를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}")
    public PartDetailResponse get(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "공식 리비전 이력 목록과 이전 리비전 대비 요약 diff를 조회합니다", description = "공식 리비전 이력 목록과 이전 리비전 대비 요약 diff를 조회합니다")
    @GetMapping("/{partNumber}/history")
    public PartRevisionHistoryResponse getHistory(
            @Parameter(description = "품번")
            @PathVariable String partNumber
    ) {
        return toPartRevisionHistoryResponse(partQuery.getHistory(new PartRevisionHistoryCondition(partNumber)));
    }

    @Operation(summary = "이전 또는 지정한 기준 리비전 대비 상세 diff를 조회합니다", description = "이전 또는 지정한 기준 리비전 대비 상세 diff를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/diff")
    public PartRevisionDiffResponse getDiff(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "비교 기준 리비전 코드")
            @RequestParam(value = "base_revision_code", required = false) String baseRevisionCode
    ) {
        return toPartRevisionDiffResponse(partQuery.getDiff(new PartRevisionDiffCondition(
                partNumber,
                revisionCode,
                baseRevisionCode
        )));
    }

    @Operation(summary = "공식 리비전을 직접 릴리즈합니다", description = "공식 리비전을 직접 릴리즈합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "릴리즈 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 릴리즈 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 릴리즈됨, 최신 승인 리비전 아님, 현재 상태에서 릴리즈 불가 등 리소스 상태 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_state",
                                    value = "{\"code\":\"INVALID_STATE\",\"message\":\"현재 최신 승인 리비전이 아닙니다. 최신 승인 리비전만 릴리즈할 수 있습니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/release")
    public PartDetailResponse release(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartRevisionResult result = releasePartRevisionUseCase.execute(new ReleasePartRevisionCommand(
                partNumber,
                revisionCode,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partNumber(), result.revisionCode())));
    }

    @Operation(summary = "Part의 직접 자식/직접 부모 BOM 관계를 조회합니다", description = "Part의 직접 자식/직접 부모 BOM 관계를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/bom")
    public PartBomResponse getBom(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartBomResponse(partQuery.get(new PartBomCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "Part BOM 트리를 정전개 또는 역전개로 조회합니다", description = "Part BOM 트리를 정전개 또는 역전개로 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/bom/tree")
    public BomTreeResponse getBomTree(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "전개 방향")
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction
    ) {
        return toBomTreeResponse(partQuery.getBomTree(new BomTreeCondition(partNumber, revisionCode, direction)));
    }

    @Operation(summary = "Part BOM 트리를 Excel(.xlsx) 파일로 내보냅니다", description = "Part BOM 트리를 Excel(.xlsx) 파일로 내보냅니다")
    @GetMapping(value = "/{partNumber}/revisions/{revisionCode}/bom/tree/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportBomTree(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "전개 방향")
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction,
            @Parameter(description = "매핑 ID")
            @RequestParam(value = "mapping_id", required = false) UUID mappingId
    ) {
        byte[] content = partQuery.exportBomTree(new BomTreeExportCondition(
                partNumber,
                revisionCode,
                direction,
                mappingId
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BOM.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(summary = "해당 Part가 소속된 프로젝트 목록을 조회합니다", description = "해당 Part가 소속된 프로젝트 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/projects")
    public PartProjectsResponse getProjects(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartProjectsResponse(partQuery.get(new PartProjectsCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "Part에 연결된 공급사 목록을 조회합니다", description = "Part에 연결된 공급사 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/suppliers")
    public PartSuppliersResponse getSuppliers(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartSuppliersResponse(partQuery.get(new PartSuppliersCondition(partNumber, revisionCode)));
    }
}
