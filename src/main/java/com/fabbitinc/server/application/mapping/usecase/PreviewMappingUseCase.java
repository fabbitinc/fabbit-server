package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.MappingLlmGenerationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.command.PreviewMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewMappingResult;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewSheetResult;
import com.fabbitinc.server.application.mapping.usecase.result.SkippedSheetResult;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.file.model.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional
public class PreviewMappingUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final MappingService mappingService;
    private final MappingLlmGenerationSupport mappingLlmGenerationSupport;
    private final OrganizationApi organizationApi;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;

    public PreviewMappingResult execute(PreviewMappingCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        organizationApi.checkCreditQuota(auth.orgId(), AiUsageCategory.BOM_ANALYSIS);

        File file = mappingService.getUploadedFileOrThrow(command.fileId());
        List<String> targetSheets = mappingService.loadPreviewTargets(file, command.sheetName());

        List<PreviewSheetResult> sheetResponses = new ArrayList<>();
        List<SkippedSheetResult> skipped = new ArrayList<>();

        List<String> firstHeaders = List.of();
        List<JsonNode> firstRows = List.of();
        MappingResultDto firstMapping = null;

        for (String sheet : targetSheets) {
            SpreadsheetParserSupport.ParsedSheet parsed;
            try {
                parsed = mappingService.loadHeadersAndRows(file, sheet, 5);
            } catch (RuntimeException ex) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResult(sheet, "파싱 실패: " + ex.getMessage()));
                }
                continue;
            }

            if (parsed.headers().isEmpty()) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResult(sheet, "헤더를 추출할 수 없습니다"));
                }
                continue;
            }

            MappingLlmGenerationSupport.GenerationOutput generation = mappingLlmGenerationSupport.generate(parsed.headers(), parsed.rows());
            organizationApi.consumeCredits(auth.orgId(), AiUsageCategory.BOM_ANALYSIS);
            aiUsageService.record(new RecordAiUsageInput(
                    auth.orgId(),
                    auth.userId(),
                    AiUsageCategory.BOM_ANALYSIS,
                    "mapping:preview",
                    generation.model(),
                    generation.inputTokens(),
                    generation.outputTokens()
            ));

            MappingResultDto normalized = generation.mapping();
            if (normalized.propertyMappings().isEmpty()) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResult(sheet, "온톨로지에 매핑 가능한 컬럼이 없습니다"));
                }
                continue;
            }

            List<JsonNode> sampleRows = toJsonRows(parsed.rows());
            if (sheet != null) {
                sheetResponses.add(new PreviewSheetResult(
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

        return new PreviewMappingResult(
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
