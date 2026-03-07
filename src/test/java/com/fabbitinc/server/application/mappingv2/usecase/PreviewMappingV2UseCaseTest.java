package com.fabbitinc.server.application.mappingv2.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.dto.common.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.support.MappingV2LlmGenerationSupport;
import com.fabbitinc.server.application.mappingv2.usecase.command.PreviewMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.result.PreviewMappingV2Result;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import tools.jackson.databind.ObjectMapper;

class PreviewMappingV2UseCaseTest {

    @Test
    void execute_llm결과에대해_quota차감과_usage기록을_수행한다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        MappingService mappingService = mock(MappingService.class);
        MappingV2LlmGenerationSupport mappingV2LlmGenerationSupport = mock(MappingV2LlmGenerationSupport.class);
        OrganizationApi organizationApi = mock(OrganizationApi.class);
        AiUsageService aiUsageService = mock(AiUsageService.class);

        PreviewMappingV2UseCase useCase = new PreviewMappingV2UseCase(
                currentAuthProvider,
                mappingService,
                mappingV2LlmGenerationSupport,
                organizationApi,
                aiUsageService,
                new ObjectMapper()
        );

        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        AuthContext auth = new AuthContext(userId, "user@fabbit.io", orgId, MembershipRole.OWNER);
        when(currentAuthProvider.getCurrentAuth()).thenReturn(auth);

        File file = File.create(fileId, "sample.xlsx", "tenants/org/raw_data/sample.xlsx", "application/vnd.ms-excel", 100L);
        file.markUploaded();

        List<String> headers = List.of("품번");
        List<Map<String, Object>> rows = List.of(Map.of("품번", "A-001"));
        MappingV2ResultDto generatedMapping = new MappingV2ResultDto(
                List.of(new NodeMappingV2Dto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm result")),
                List.of()
        );

        when(mappingService.getUploadedFileOrThrow(fileId)).thenReturn(file);
        when(mappingService.loadPreviewTargets(file, null)).thenReturn(List.of("Sheet1"));
        when(mappingService.loadHeadersAndRows(file, "Sheet1", 5))
                .thenReturn(new SpreadsheetParserSupport.ParsedSheet(headers, rows));
        when(mappingV2LlmGenerationSupport.generate(headers, rows))
                .thenReturn(new MappingV2LlmGenerationSupport.GenerationOutput(generatedMapping, "openai/gpt-5-mini", 123, 45));

        PreviewMappingV2Result result = useCase.execute(new PreviewMappingV2Command(fileId, null));

        assertEquals(headers, result.headers());
        assertEquals(1, result.sheets().size());
        assertEquals(generatedMapping, result.mapping());

        InOrder inOrder = inOrder(organizationApi, mappingV2LlmGenerationSupport, aiUsageService);
        inOrder.verify(organizationApi).checkCreditQuota(orgId, AiUsageCategory.BOM_ANALYSIS);
        inOrder.verify(mappingV2LlmGenerationSupport).generate(headers, rows);
        inOrder.verify(organizationApi).consumeCredits(orgId, AiUsageCategory.BOM_ANALYSIS);
        inOrder.verify(aiUsageService).record(org.mockito.ArgumentMatchers.any(RecordAiUsageInput.class));

        ArgumentCaptor<RecordAiUsageInput> usageCaptor = ArgumentCaptor.forClass(RecordAiUsageInput.class);
        verify(aiUsageService).record(usageCaptor.capture());
        assertEquals("mapping-v2:preview", usageCaptor.getValue().feature());
    }
}
