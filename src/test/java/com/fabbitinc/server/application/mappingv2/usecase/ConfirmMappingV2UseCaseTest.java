package com.fabbitinc.server.application.mappingv2.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.dto.common.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.service.MappingV2Service;
import com.fabbitinc.server.application.mappingv2.service.output.SavedMappingV2Output;
import com.fabbitinc.server.application.mappingv2.support.MappingV2NormalizationSupport;
import com.fabbitinc.server.application.mappingv2.support.MappingV2ValidationSupport;
import com.fabbitinc.server.application.mappingv2.usecase.command.ConfirmMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.result.SavedMappingV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.support.SavedMappingV2ResultMapper;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ConfirmMappingV2UseCaseTest {

    @Test
    void execute_검증을통과하면_v2매핑을저장한다() {
        MappingService mappingService = mock(MappingService.class);
        MappingV2Service mappingV2Service = mock(MappingV2Service.class);
        MappingV2NormalizationSupport normalizationSupport = mock(MappingV2NormalizationSupport.class);
        MappingV2ValidationSupport validationSupport = mock(MappingV2ValidationSupport.class);

        ConfirmMappingV2UseCase useCase = new ConfirmMappingV2UseCase(
                mappingService,
                mappingV2Service,
                normalizationSupport,
                validationSupport,
                new SavedMappingV2ResultMapper(new ObjectMapper())
        );

        UUID fileId = UUID.randomUUID();
        File file = File.create(fileId, "sample.xlsx", "tenants/test/sample.xlsx", "application/vnd.ms-excel", 10L);
        file.markUploaded();

        MappingV2ResultDto rawMapping = new MappingV2ResultDto(
                List.of(new NodeMappingV2Dto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                List.of()
        );
        MappingV2ResultDto normalized = new MappingV2ResultDto(
                List.of(new NodeMappingV2Dto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "normalized")),
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
                .thenReturn(new MappingV2ValidationSupport.ValidationResult(
                        List.of(),
                        List.of(),
                        new MappingV2ValidationSupport.ValidationImpactSummary(0)
                ));

        MappingV2Record record = MappingV2Record.create("V2 기본 매핑");
        MappingV2Revision revision = record.createRevision(
                fileId,
                "Sheet1",
                "[\"품번\"]",
                "{\"nodes\":[{\"nodeId\":\"part_main\",\"label\":\"Part\",\"propertyColumns\":{\"part_number\":\"품번\"},\"extendedProperties\":[],\"confidence\":95,\"reason\":\"normalized\"}],\"relations\":[]}"
        );
        when(mappingV2Service.createMapping(any()))
                .thenReturn(new SavedMappingV2Output(record, revision));

        SavedMappingV2Result result = useCase.execute(new ConfirmMappingV2Command(
                fileId,
                "V2 기본 매핑",
                "Sheet1",
                rawMapping
        ));

        assertEquals("V2 기본 매핑", result.name());
        assertEquals(1, result.version());
        assertEquals(normalized.requiredColumns(), result.mappedHeaders());
        verify(mappingV2Service).createMapping(any());
    }

    @Test
    void execute_검증오류가있으면_저장하지않고_예외를던진다() {
        MappingService mappingService = mock(MappingService.class);
        MappingV2Service mappingV2Service = mock(MappingV2Service.class);
        MappingV2NormalizationSupport normalizationSupport = mock(MappingV2NormalizationSupport.class);
        MappingV2ValidationSupport validationSupport = mock(MappingV2ValidationSupport.class);

        ConfirmMappingV2UseCase useCase = new ConfirmMappingV2UseCase(
                mappingService,
                mappingV2Service,
                normalizationSupport,
                validationSupport,
                new SavedMappingV2ResultMapper(new ObjectMapper())
        );

        UUID fileId = UUID.randomUUID();
        File file = File.create(fileId, "sample.xlsx", "tenants/test/sample.xlsx", "application/vnd.ms-excel", 10L);
        file.markUploaded();

        MappingV2ResultDto mapping = new MappingV2ResultDto(
                List.of(new NodeMappingV2Dto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
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
                .thenReturn(new MappingV2ValidationSupport.ValidationResult(
                        List.of(new MappingV2ValidationSupport.ValidationIssue(
                                "MISSING_SOURCE_COLUMN",
                                "error",
                                "컬럼을 찾을 수 없습니다",
                                "nodes[0].property_columns.part_number",
                                null
                        )),
                        List.of(),
                        new MappingV2ValidationSupport.ValidationImpactSummary(1)
                ));

        AppException exception = assertThrows(AppException.class, () -> useCase.execute(new ConfirmMappingV2Command(
                fileId,
                "V2 기본 매핑",
                null,
                mapping
        )));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(mappingV2Service, never()).createMapping(any());
    }
}
