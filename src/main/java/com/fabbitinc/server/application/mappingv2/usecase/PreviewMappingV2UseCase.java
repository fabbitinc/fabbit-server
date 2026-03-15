package com.fabbitinc.server.application.mappingv2.usecase;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.result.SkippedSheetResult;
import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.support.MappingV2LlmGenerationSupport;
import com.fabbitinc.server.application.mappingv2.usecase.command.PreviewMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.result.PreviewMappingV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.result.PreviewSheetV2Result;
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
public class PreviewMappingV2UseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final MappingService mappingService;
    private final MappingV2LlmGenerationSupport mappingV2LlmGenerationSupport;
    private final OrganizationApi organizationApi;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;

    public PreviewMappingV2Result execute(PreviewMappingV2Command command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        organizationApi.checkCreditQuota(auth.orgId(), AiUsageCategory.BOM_ANALYSIS);

        File file = mappingService.getUploadedFileOrThrow(command.fileId());
        List<String> targetSheets = mappingService.loadPreviewTargets(file, command.sheetName());

        List<PreviewSheetV2Result> sheetResponses = new ArrayList<>();
        List<SkippedSheetResult> skipped = new ArrayList<>();

        List<String> firstHeaders = List.of();
        List<JsonNode> firstRows = List.of();
        MappingV2ResultDto firstMapping = null;

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

            MappingV2LlmGenerationSupport.GenerationOutput generation = mappingV2LlmGenerationSupport.generate(
                    parsed.headers(),
                    parsed.rows()
            );
            organizationApi.consumeCredits(auth.orgId(), AiUsageCategory.BOM_ANALYSIS);
            aiUsageService.record(new RecordAiUsageInput(
                    auth.orgId(),
                    auth.userId(),
                    AiUsageCategory.BOM_ANALYSIS,
                    "mapping-v2:preview",
                    generation.model(),
                    generation.inputTokens(),
                    generation.outputTokens()
            ));

            MappingV2ResultDto normalized = generation.mapping();
            if (normalized.nodes().isEmpty()) {
                if (sheet != null) {
                    skipped.add(new SkippedSheetResult(sheet, "온톨로지에 매핑 가능한 노드가 없습니다"));
                }
                continue;
            }

            List<JsonNode> sampleRows = toJsonRows(parsed.rows());
            if (sheet != null) {
                sheetResponses.add(new PreviewSheetV2Result(
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

        return new PreviewMappingV2Result(
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
