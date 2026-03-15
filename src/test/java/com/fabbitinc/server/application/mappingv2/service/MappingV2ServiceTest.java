package com.fabbitinc.server.application.mappingv2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.model.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.service.input.CreateMappingV2Input;
import com.fabbitinc.server.application.mappingv2.service.input.UpdateMappingV2Input;
import com.fabbitinc.server.application.mappingv2.service.output.SavedMappingV2Output;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RecordRepository;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RevisionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class MappingV2ServiceTest {

    @Test
    void createMapping_v2매핑과_리비전을_저장한다() {
        MappingV2RecordRepository recordRepository = mock(MappingV2RecordRepository.class);
        MappingV2RevisionRepository revisionRepository = mock(MappingV2RevisionRepository.class);
        MappingV2Service service = new MappingV2Service(recordRepository, revisionRepository, new ObjectMapper());

        when(recordRepository.existsByName("V2 기본 매핑")).thenReturn(false);

        UUID fileId = UUID.randomUUID();
        MappingV2ResultDto mapping = new MappingV2ResultDto(
                List.of(new NodeMappingV2Dto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                List.of()
        );

        SavedMappingV2Output output = service.createMapping(new CreateMappingV2Input(
                "V2 기본 매핑",
                fileId,
                "Sheet1",
                List.of("품번"),
                mapping
        ));

        assertEquals("V2 기본 매핑", output.record().getName());
        assertEquals(fileId, output.revision().getFileId());
        assertEquals(1, output.revision().getVersion());

        ArgumentCaptor<MappingV2Record> recordCaptor = ArgumentCaptor.forClass(MappingV2Record.class);
        verify(recordRepository).save(recordCaptor.capture());
        assertEquals("V2 기본 매핑", recordCaptor.getValue().getName());

        ArgumentCaptor<MappingV2Revision> revisionCaptor = ArgumentCaptor.forClass(MappingV2Revision.class);
        verify(revisionRepository).save(revisionCaptor.capture());
        assertEquals("Sheet1", revisionCaptor.getValue().getSheetName());
        assertEquals(fileId, revisionCaptor.getValue().getFileId());
    }

    @Test
    void createMapping_같은이름이이미있으면_충돌예외를던진다() {
        MappingV2RecordRepository recordRepository = mock(MappingV2RecordRepository.class);
        MappingV2RevisionRepository revisionRepository = mock(MappingV2RevisionRepository.class);
        MappingV2Service service = new MappingV2Service(recordRepository, revisionRepository, new ObjectMapper());

        when(recordRepository.existsByName("V2 기본 매핑")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> service.createMapping(new CreateMappingV2Input(
                "V2 기본 매핑",
                UUID.randomUUID(),
                null,
                List.of("품번"),
                new MappingV2ResultDto(List.of(), List.of())
        )));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(revisionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void updateMapping_이름과_리비전을_갱신한다() {
        MappingV2RecordRepository recordRepository = mock(MappingV2RecordRepository.class);
        MappingV2RevisionRepository revisionRepository = mock(MappingV2RevisionRepository.class);
        MappingV2Service service = new MappingV2Service(recordRepository, revisionRepository, new ObjectMapper());

        MappingV2Record record = MappingV2Record.create("기존 매핑");
        record.createRevision(UUID.randomUUID(), null, "[]", "{}");

        when(recordRepository.findById(record.getId())).thenReturn(java.util.Optional.of(record));
        when(recordRepository.existsByNameAndIdNot("새 매핑", record.getId())).thenReturn(false);

        SavedMappingV2Output output = service.updateMapping(record.getId(), new UpdateMappingV2Input(
                "새 매핑",
                UUID.randomUUID(),
                "Sheet2",
                List.of("품번"),
                new MappingV2ResultDto(
                        List.of(new NodeMappingV2Dto("part_main", "Part", Map.of("part_number", "품번"), List.of(), 95, "llm")),
                        List.of()
                )
        ));

        assertEquals("새 매핑", output.record().getName());
        assertEquals(2, output.revision().getVersion());
        assertEquals("Sheet2", output.revision().getSheetName());
        verify(revisionRepository).save(any(MappingV2Revision.class));
    }

    @Test
    void deactivateMapping_활성매핑을_비활성화한다() {
        MappingV2RecordRepository recordRepository = mock(MappingV2RecordRepository.class);
        MappingV2RevisionRepository revisionRepository = mock(MappingV2RevisionRepository.class);
        MappingV2Service service = new MappingV2Service(recordRepository, revisionRepository, new ObjectMapper());

        MappingV2Record record = MappingV2Record.create("기존 매핑");
        when(recordRepository.findById(record.getId())).thenReturn(java.util.Optional.of(record));

        service.deactivateMapping(record.getId());

        assertEquals(false, record.isActive());
    }
}
