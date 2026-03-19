package com.fabbitinc.server.application.mapping.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.NodeMappingDto;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.service.output.SavedMappingOutput;
import com.fabbitinc.server.application.mapping.support.MappingNormalizationSupport;
import com.fabbitinc.server.application.mapping.support.MappingValidationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.command.ConfirmMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.result.SavedMappingResult;
import com.fabbitinc.server.application.mapping.usecase.support.SavedMappingResultMapper;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ConfirmMappingUseCaseTest {

    @Test
    void execute_검증을통과하면_v2매핑을저장한다() {
        MappingService mappingService = mock(MappingService.class);
        MappingNormalizationSupport normalizationSupport = mock(MappingNormalizationSupport.class);
        MappingValidationSupport validationSupport = mock(MappingValidationSupport.class);

        ConfirmMappingUseCase useCase = new ConfirmMappingUseCase(
                mappingService,
                normalizationSupport,
                validationSupport,
                new SavedMappingResultMapper(new ObjectMapper())
        );

        UUID fileId = UUID.randomUUID();
        File file = File.create(fileId, "sample.xlsx", "tenants/test/sample.xlsx", "application/vnd.ms-excel", 10L);
        file.markUploaded();

        MappingResultDto rawMapping = new MappingResultDto(
                List.of(new NodeMappingDto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                List.of()
        );
        MappingResultDto normalized = new MappingResultDto(
                List.of(new NodeMappingDto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "normalized")),
                List.of()
        );

        when(mappingService.getUploadedFileOrThrow(fileId)).thenReturn(file);
        when(mappingService.loadHeadersAndRows(file, "Sheet1", 30))
                .thenReturn(new SpreadsheetParserSupport.ParsedSheet(
                        List.of("품번"),
                        List.of(Map.of("품번", "A-001"))
                ));
        when(normalizationSupport.normalize(rawMapping)).thenReturn(normalized);
        when(validationSupport.validateAgainstRows(any(), any(), any()))
                .thenReturn(new MappingValidationSupport.ValidationResult(
                        List.of(),
                        List.of(),
                        new MappingValidationSupport.ValidationImpactSummary(0)
                ));

        MappingRecord record = MappingRecord.create("V2 기본 매핑");
        MappingRevision revision = record.createRevision(
                fileId,
                "Sheet1",
                "[\"품번\"]",
                "{\"nodes\":[{\"nodeId\":\"part_main\",\"label\":\"Part\",\"propertyColumns\":{\"part_number\":\"품번\"},\"extendedProperties\":[],\"confidence\":95,\"reason\":\"normalized\"}],\"relations\":[]}"
        );
        when(mappingService.createMapping(any()))
                .thenReturn(new SavedMappingOutput(record, revision));

        SavedMappingResult result = useCase.execute(new ConfirmMappingCommand(
                fileId,
                "V2 기본 매핑",
                "Sheet1",
                rawMapping
        ));

        assertEquals("V2 기본 매핑", result.name());
        assertEquals(1, result.version());
        assertEquals(normalized.requiredColumns(), result.mappedHeaders());
        verify(mappingService).createMapping(any());
    }

    @Test
    void execute_검증오류가있으면_저장하지않고_예외를던진다() {
        MappingService mappingService = mock(MappingService.class);
        MappingNormalizationSupport normalizationSupport = mock(MappingNormalizationSupport.class);
        MappingValidationSupport validationSupport = mock(MappingValidationSupport.class);

        ConfirmMappingUseCase useCase = new ConfirmMappingUseCase(
                mappingService,
                normalizationSupport,
                validationSupport,
                new SavedMappingResultMapper(new ObjectMapper())
        );

        UUID fileId = UUID.randomUUID();
        File file = File.create(fileId, "sample.xlsx", "tenants/test/sample.xlsx", "application/vnd.ms-excel", 10L);
        file.markUploaded();

        MappingResultDto mapping = new MappingResultDto(
                List.of(new NodeMappingDto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                List.of()
        );

        when(mappingService.getUploadedFileOrThrow(fileId)).thenReturn(file);
        when(mappingService.loadHeadersAndRows(file, null, 30))
                .thenReturn(new SpreadsheetParserSupport.ParsedSheet(
                        List.of("다른컬럼"),
                        List.of(Map.of("다른컬럼", "A-001"))
                ));
        when(normalizationSupport.normalize(mapping)).thenReturn(mapping);
        when(validationSupport.validateAgainstRows(any(), any(), any()))
                .thenReturn(new MappingValidationSupport.ValidationResult(
                        List.of(new MappingValidationSupport.ValidationIssue(
                                "MISSING_SOURCE_COLUMN",
                                "error",
                                "컬럼을 찾을 수 없습니다",
                                "nodes[0].property_columns.part_number",
                                null
                        )),
                        List.of(),
                        new MappingValidationSupport.ValidationImpactSummary(1)
                ));

        AppException exception = assertThrows(AppException.class, () -> useCase.execute(new ConfirmMappingCommand(
                fileId,
                "V2 기본 매핑",
                null,
                mapping
        )));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(mappingService, never()).createMapping(any());
    }
}
