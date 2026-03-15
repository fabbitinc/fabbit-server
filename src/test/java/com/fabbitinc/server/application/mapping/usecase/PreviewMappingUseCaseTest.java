package com.fabbitinc.server.application.mapping.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.aiusage.service.AiUsageService;
import com.fabbitinc.server.application.aiusage.service.input.RecordAiUsageInput;
import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.MappingLlmGenerationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.command.PreviewMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewMappingResult;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
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

class PreviewMappingUseCaseTest {

    @Test
    void execute_llm결과에대해_quota차감과_usage기록을_수행한다() {
        CurrentAuthProvider currentAuthProvider = mock(CurrentAuthProvider.class);
        MappingService mappingService = mock(MappingService.class);
        MappingLlmGenerationSupport mappingLlmGenerationSupport = mock(MappingLlmGenerationSupport.class);
        OrganizationApi organizationApi = mock(OrganizationApi.class);
        AiUsageService aiUsageService = mock(AiUsageService.class);

        PreviewMappingUseCase useCase = new PreviewMappingUseCase(
                currentAuthProvider,
                mappingService,
                mappingLlmGenerationSupport,
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
        MappingResultDto generatedMapping = new MappingResultDto(
                List.of(new PropertyMappingDto("품번", "part_number", "_ext_number", PropertyDataType.STRING, 95, "llm result", false)),
                List.of()
        );

        when(mappingService.getUploadedFileOrThrow(fileId)).thenReturn(file);
        when(mappingService.loadPreviewTargets(file, null)).thenReturn(List.of("Sheet1"));
        when(mappingService.loadHeadersAndRows(file, "Sheet1", 5))
                .thenReturn(new SpreadsheetParserSupport.ParsedSheet(headers, rows));
        when(mappingLlmGenerationSupport.generate(headers, rows))
                .thenReturn(new MappingLlmGenerationSupport.GenerationOutput(generatedMapping, "openai/gpt-5-mini", 123, 45));

        PreviewMappingResult result = useCase.execute(new PreviewMappingCommand(fileId, null));

        assertEquals(headers, result.headers());
        assertEquals(1, result.sheets().size());
        assertEquals(generatedMapping, result.mapping());
        assertEquals("_ext_number", result.mapping().propertyMappings().get(0).suggestedExtendedProperty());

        InOrder inOrder = inOrder(organizationApi, mappingLlmGenerationSupport, aiUsageService);
        inOrder.verify(organizationApi).checkCreditQuota(orgId, AiUsageCategory.BOM_ANALYSIS);
        inOrder.verify(mappingLlmGenerationSupport).generate(headers, rows);
        inOrder.verify(organizationApi).consumeCredits(orgId, AiUsageCategory.BOM_ANALYSIS);
        inOrder.verify(aiUsageService).record(org.mockito.ArgumentMatchers.any(RecordAiUsageInput.class));

        ArgumentCaptor<RecordAiUsageInput> usageCaptor = ArgumentCaptor.forClass(RecordAiUsageInput.class);
        verify(aiUsageService).record(usageCaptor.capture());
        RecordAiUsageInput usageInput = usageCaptor.getValue();
        assertEquals(orgId, usageInput.orgId());
        assertEquals(userId, usageInput.userId());
        assertEquals(AiUsageCategory.BOM_ANALYSIS, usageInput.category());
        assertEquals("mapping:preview", usageInput.feature());
        assertEquals("openai/gpt-5-mini", usageInput.model());
        assertEquals(123, usageInput.inputTokens());
        assertEquals(45, usageInput.outputTokens());
    }
}
