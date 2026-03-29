package com.fabbitinc.server.presentation.bom.controller;

import com.fabbitinc.server.application.bom.query.BomCompareQuery;
import com.fabbitinc.server.application.bom.query.WhereUsedSummaryQuery;
import com.fabbitinc.server.application.bom.query.condition.BomCompareCondition;
import com.fabbitinc.server.application.bom.query.condition.BomCompareExportCondition;
import com.fabbitinc.server.application.bom.query.condition.WhereUsedSummaryCondition;
import com.fabbitinc.server.application.bom.query.result.BomCompareResult;
import com.fabbitinc.server.application.bom.query.result.WhereUsedSummaryResult;
import com.fabbitinc.server.presentation.bom.request.BomCompareRequest;
import com.fabbitinc.server.presentation.bom.response.BomCompareResponse;
import com.fabbitinc.server.presentation.bom.response.WhereUsedSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "bom-queries", description = "BOM 비교 및 Where-used 조회 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class BomQueryController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final BomCompareQuery bomCompareQuery;
    private final WhereUsedSummaryQuery whereUsedSummaryQuery;

    @Operation(summary = "두 리비전의 BOM을 비교합니다", description = "소스/대상 리비전의 BOM 항목을 LINE_NUMBER 기준으로 비교하여 변경 목록과 요약을 반환합니다")
    @PostMapping("/bom/compare")
    public BomCompareResponse compare(@Valid @RequestBody BomCompareRequest request) {
        BomCompareResult result = bomCompareQuery.compare(new BomCompareCondition(
                request.sourceRevisionId(),
                request.targetRevisionId()
        ));
        return toBomCompareResponse(result);
    }

    @Operation(summary = "BOM 비교 결과를 Excel로 내보냅니다", description = "소스/대상 리비전의 BOM 비교 결과를 Excel(.xlsx) 파일로 내보냅니다")
    @PostMapping(value = "/bom/compare/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportCompare(@Valid @RequestBody BomCompareRequest request) {
        byte[] content = bomCompareQuery.exportExcel(new BomCompareExportCondition(
                request.sourceRevisionId(),
                request.targetRevisionId()
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BOM_Compare.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(summary = "Where-used 요약을 조회합니다", description = "해당 리비전을 하위 부품으로 참조하는 상위 리비전의 집계 정보를 반환합니다")
    @GetMapping("/parts/{partId}/revisions/{revisionId}/bom/where-used/summary")
    public WhereUsedSummaryResponse getWhereUsedSummary(
            @Parameter(description = "부품 ID") @PathVariable UUID partId,
            @Parameter(description = "리비전 ID") @PathVariable UUID revisionId
    ) {
        WhereUsedSummaryResult result = whereUsedSummaryQuery.get(new WhereUsedSummaryCondition(partId, revisionId));
        return toWhereUsedSummaryResponse(result);
    }

    private BomCompareResponse toBomCompareResponse(BomCompareResult result) {
        return new BomCompareResponse(
                result.changes().stream()
                        .map(change -> new BomCompareResponse.Change(
                                change.lineNumber(),
                                change.changeType(),
                                change.sourcePartNumber(),
                                change.sourceName(),
                                change.sourceQuantity(),
                                change.targetPartNumber(),
                                change.targetName(),
                                change.targetQuantity()
                        ))
                        .toList(),
                new BomCompareResponse.Summary(
                        result.summary().addedCount(),
                        result.summary().removedCount(),
                        result.summary().changedCount(),
                        result.summary().unchangedCount(),
                        result.summary().totalCount()
                )
        );
    }

    private WhereUsedSummaryResponse toWhereUsedSummaryResponse(WhereUsedSummaryResult result) {
        return new WhereUsedSummaryResponse(
                result.directReferenceCount(),
                new WhereUsedSummaryResponse.StatusBreakdown(
                        result.statusBreakdown().draftCount(),
                        result.statusBreakdown().releasedCount(),
                        result.statusBreakdown().supersededCount(),
                        result.statusBreakdown().canceledCount()
                ),
                result.references().stream()
                        .map(ref -> new WhereUsedSummaryResponse.Reference(
                                ref.partId(),
                                ref.partNumber(),
                                ref.partName(),
                                ref.revisionId(),
                                ref.revisionCode(),
                                ref.revisionStatus()
                        ))
                        .toList()
        );
    }
}
