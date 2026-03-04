package com.fabbitinc.server.application.mapping.usecase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.request.MappingPreviewRequest;
import com.fabbitinc.server.application.mapping.dto.response.MappingPreviewResponse;
import com.fabbitinc.server.application.mapping.dto.response.SheetPreviewResponse;
import com.fabbitinc.server.application.mapping.dto.response.SkippedSheetResponse;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.MappingGenerationSupport;
import com.fabbitinc.server.application.mapping.support.MappingNormalizationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PreviewMappingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final MappingService mappingService;
    private final MappingGenerationSupport mappingGenerationSupport;
    private final MappingNormalizationSupport mappingNormalizationSupport;
    private final ObjectMapper objectMapper;

    @Transactional
    public MappingPreviewResponse execute(MappingPreviewRequest request) {
        currentAuthProvider.getCurrentAuth();

        File file = mappingService.getUploadedFileOrThrow(request.fileId());
        List<String> targetSheets = mappingService.loadPreviewTargets(file, request.sheetName());

        List<SheetPreviewResponse> sheetResponses = new ArrayList<>();
        List<SkippedSheetResponse> skipped = new ArrayList<>();

        List<String> firstHeaders = List.of();
        List<JsonNode> firstRows = List.of();
        MappingResultDto firstMapping = null;

        for (String sheet : targetSheets) {
            SpreadsheetParserSupport.ParsedSheet parsed;
            try {
                parsed = mappingService.loadHeadersAndRows(file, sheet, 5);
            } catch (RuntimeException ex) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResponse(sheet, "파싱 실패: " + ex.getMessage()));
                }
                continue;
            }

            if (parsed.headers().isEmpty()) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResponse(sheet, "헤더를 추출할 수 없습니다"));
                }
                continue;
            }

            MappingResultDto generated = mappingGenerationSupport.generate(parsed.headers(), parsed.rows());
            MappingResultDto normalized = mappingNormalizationSupport.normalize(generated);
            if (normalized.propertyMappings().isEmpty()) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResponse(sheet, "온톨로지에 매핑 가능한 컬럼이 없습니다"));
                }
                continue;
            }

            List<JsonNode> sampleRows = toJsonRows(parsed.rows());
            if (sheet != null) {
                sheetResponses.add(new SheetPreviewResponse(
                        sheet,
                        parsed.headers(),
                        sampleRows,
                        normalized
                ));
            }

            if (firstMapping == null) {
                firstHeaders = parsed.headers();
                firstRows = sampleRows;
                firstMapping = normalized;
            }
        }

        if (firstMapping == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "파일에서 매핑 가능한 데이터를 찾을 수 없습니다");
        }

        return new MappingPreviewResponse(
                firstHeaders,
                firstRows,
                firstMapping,
                sheetResponses,
                skipped
        );
    }

    private List<JsonNode> toJsonRows(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> (JsonNode) objectMapper.valueToTree(row))
                .toList();
    }
}
