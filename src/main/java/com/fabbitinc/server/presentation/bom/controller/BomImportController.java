package com.fabbitinc.server.presentation.bom.controller;

import com.fabbitinc.server.application.bom.usecase.CommitBomImportUseCase;
import com.fabbitinc.server.application.bom.usecase.PreviewBomImportUseCase;
import com.fabbitinc.server.application.bom.usecase.command.CommitBomImportCommand;
import com.fabbitinc.server.application.bom.usecase.command.PreviewBomImportCommand;
import com.fabbitinc.server.application.bom.usecase.result.CommitBomImportResult;
import com.fabbitinc.server.application.bom.usecase.result.PreviewBomImportResult;
import com.fabbitinc.server.presentation.bom.request.CommitBomImportRequest;
import com.fabbitinc.server.presentation.bom.request.PreviewBomImportRequest;
import com.fabbitinc.server.presentation.bom.response.BomImportCommitResponse;
import com.fabbitinc.server.presentation.bom.response.BomImportPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "bom-import", description = "BOM 가져오기 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class BomImportController {

    private static final String TEMPLATE_FILENAME = "bom_import_template.xlsx";
    private static final String[] TEMPLATE_HEADERS = {"line_number", "child_part_number", "child_revision_code", "quantity"};

    private final PreviewBomImportUseCase previewBomImportUseCase;
    private final CommitBomImportUseCase commitBomImportUseCase;

    @Operation(
            operationId = "bomImportDownloadTemplate",
            summary = "BOM 가져오기 템플릿을 다운로드합니다",
            description = "BOM 가져오기에 사용할 엑셀 템플릿 파일을 다운로드합니다. 헤더 행만 포함된 .xlsx 파일을 반환합니다"
    )
    @GetMapping("/{partId}/revisions/{revisionId}/bom-items/import/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @Parameter(description = "부품 ID") @PathVariable UUID partId,
            @Parameter(description = "리비전 ID") @PathVariable UUID revisionId
    ) throws IOException {
        byte[] content = generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + TEMPLATE_FILENAME + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(content.length)
                .body(content);
    }

    @Operation(
            operationId = "bomImportPreview",
            summary = "BOM 가져오기 미리보기를 실행합니다",
            description = "업로드된 엑셀 파일의 BOM 데이터를 검증하고 행별 결과를 반환합니다. DRAFT 상태의 리비전에서만 사용 가능합니다"
    )
    @PostMapping("/{partId}/revisions/{revisionId}/bom-items/import/preview")
    public BomImportPreviewResponse preview(
            @Parameter(description = "부품 ID") @PathVariable UUID partId,
            @Parameter(description = "리비전 ID") @PathVariable UUID revisionId,
            @Valid @RequestBody PreviewBomImportRequest request
    ) {
        PreviewBomImportResult result = previewBomImportUseCase.execute(new PreviewBomImportCommand(
                partId, revisionId, request.fileId()
        ));
        return toPreviewResponse(result);
    }

    @Operation(
            operationId = "bomImportCommit",
            summary = "BOM 가져오기를 확정합니다",
            description = "미리보기로 검증한 엑셀 파일의 BOM 데이터를 실제로 등록합니다. APPEND 모드는 기존 항목에 추가하고, REPLACE 모드는 기존 항목을 모두 삭제 후 등록합니다"
    )
    @PostMapping("/{partId}/revisions/{revisionId}/bom-items/import/commit")
    @ResponseStatus(HttpStatus.CREATED)
    public BomImportCommitResponse commit(
            @Parameter(description = "부품 ID") @PathVariable UUID partId,
            @Parameter(description = "리비전 ID") @PathVariable UUID revisionId,
            @Valid @RequestBody CommitBomImportRequest request
    ) {
        CommitBomImportResult result = commitBomImportUseCase.execute(new CommitBomImportCommand(
                partId, revisionId, request.fileId(), request.mode()
        ));
        return toCommitResponse(result);
    }

    private byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("BOM");
            XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(TEMPLATE_HEADERS[i]);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private BomImportPreviewResponse toPreviewResponse(PreviewBomImportResult result) {
        return new BomImportPreviewResponse(
                result.rows().stream()
                        .map(row -> new BomImportPreviewResponse.RowResult(
                                row.rowNumber(),
                                row.lineNumber(),
                                row.childPartNumber(),
                                row.childRevisionCode(),
                                row.quantity(),
                                row.status().name(),
                                row.message()
                        ))
                        .toList(),
                new BomImportPreviewResponse.SummaryResponse(
                        result.summary().totalCount(),
                        result.summary().successCount(),
                        result.summary().errorCount(),
                        result.summary().warningCount()
                )
        );
    }

    private BomImportCommitResponse toCommitResponse(CommitBomImportResult result) {
        return new BomImportCommitResponse(
                result.createdBomItemIds(),
                new BomImportCommitResponse.SummaryResponse(
                        result.summary().totalCreated(),
                        result.summary().mode().name()
                )
        );
    }
}
