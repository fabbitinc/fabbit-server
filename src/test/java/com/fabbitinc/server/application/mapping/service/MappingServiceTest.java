package com.fabbitinc.server.application.mapping.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.NodeMappingDto;
import com.fabbitinc.server.application.mapping.service.input.CreateMappingInput;
import com.fabbitinc.server.application.mapping.service.input.UpdateMappingInput;
import com.fabbitinc.server.application.mapping.service.output.SavedMappingOutput;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRecordRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class MappingServiceTest {

    @Test
    void createMapping_v2매핑과_리비전을_저장한다() {
        MappingRecordRepository recordRepository = mock(MappingRecordRepository.class);
        MappingRevisionRepository revisionRepository = mock(MappingRevisionRepository.class);
        MappingService service = new MappingService(recordRepository, revisionRepository, mock(FileRepository.class), mock(StoragePort.class), mock(SpreadsheetParserSupport.class), new ObjectMapper());

        when(recordRepository.existsByName("V2 기본 매핑")).thenReturn(false);

        UUID fileId = UUID.randomUUID();
        MappingResultDto mapping = new MappingResultDto(
                List.of(new NodeMappingDto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                List.of()
        );

        SavedMappingOutput output = service.createMapping(new CreateMappingInput(
                "V2 기본 매핑",
                fileId,
                "Sheet1",
                List.of("품번"),
                mapping
        ));

        assertEquals("V2 기본 매핑", output.record().getName());
        assertEquals(fileId, output.revision().getFileId());
        assertEquals(1, output.revision().getVersion());

        ArgumentCaptor<MappingRecord> recordCaptor = ArgumentCaptor.forClass(MappingRecord.class);
        verify(recordRepository).save(recordCaptor.capture());
        assertEquals("V2 기본 매핑", recordCaptor.getValue().getName());

        ArgumentCaptor<MappingRevision> revisionCaptor = ArgumentCaptor.forClass(MappingRevision.class);
        verify(revisionRepository).save(revisionCaptor.capture());
        assertEquals("Sheet1", revisionCaptor.getValue().getSheetName());
        assertEquals(fileId, revisionCaptor.getValue().getFileId());
    }

    @Test
    void createMapping_같은이름이이미있으면_충돌예외를던진다() {
        MappingRecordRepository recordRepository = mock(MappingRecordRepository.class);
        MappingRevisionRepository revisionRepository = mock(MappingRevisionRepository.class);
        MappingService service = new MappingService(recordRepository, revisionRepository, mock(FileRepository.class), mock(StoragePort.class), mock(SpreadsheetParserSupport.class), new ObjectMapper());

        when(recordRepository.existsByName("V2 기본 매핑")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> service.createMapping(new CreateMappingInput(
                "V2 기본 매핑",
                UUID.randomUUID(),
                null,
                List.of("품번"),
                new MappingResultDto(List.of(), List.of())
        )));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(revisionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateMapping_이름과_리비전을_갱신한다() {
        MappingRecordRepository recordRepository = mock(MappingRecordRepository.class);
        MappingRevisionRepository revisionRepository = mock(MappingRevisionRepository.class);
        MappingService service = new MappingService(recordRepository, revisionRepository, mock(FileRepository.class), mock(StoragePort.class), mock(SpreadsheetParserSupport.class), new ObjectMapper());

        MappingRecord record = MappingRecord.create("기존 매핑");
        record.createRevision(UUID.randomUUID(), null, "[]", "{}");

        when(recordRepository.findById(record.getId())).thenReturn(java.util.Optional.of(record));
        when(recordRepository.existsByNameAndIdNot("새 매핑", record.getId())).thenReturn(false);

        SavedMappingOutput output = service.updateMapping(record.getId(), new UpdateMappingInput(
                "새 매핑",
                UUID.randomUUID(),
                "Sheet2",
                List.of("품번"),
                new MappingResultDto(
                        List.of(new NodeMappingDto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                        List.of()
                )
        ));

        assertEquals("새 매핑", output.record().getName());
        assertEquals(2, output.revision().getVersion());
        assertEquals("Sheet2", output.revision().getSheetName());
        verify(revisionRepository).save(any(MappingRevision.class));
    }

    @Test
    void deactivateMapping_활성매핑을_비활성화한다() {
        MappingRecordRepository recordRepository = mock(MappingRecordRepository.class);
        MappingRevisionRepository revisionRepository = mock(MappingRevisionRepository.class);
        MappingService service = new MappingService(recordRepository, revisionRepository, mock(FileRepository.class), mock(StoragePort.class), mock(SpreadsheetParserSupport.class), new ObjectMapper());

        MappingRecord record = MappingRecord.create("기존 매핑");
        when(recordRepository.findById(record.getId())).thenReturn(java.util.Optional.of(record));

        service.deactivateMapping(record.getId());

        assertEquals(false, record.isActive());
    }
}
