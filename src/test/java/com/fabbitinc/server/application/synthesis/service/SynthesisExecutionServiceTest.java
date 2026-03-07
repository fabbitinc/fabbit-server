package com.fabbitinc.server.application.synthesis.service;

import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.common.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynthesisExecutionServiceTest {

    @Mock
    private SynthesisJobRepository synthesisJobRepository;
    @Mock
    private MappingRevisionRepository mappingRevisionRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private StoragePort storagePort;
    @Mock
    private SpreadsheetParserSupport spreadsheetParserSupport;
    @Mock
    private PartRepository partRepository;
    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private BomLinkRepository bomLinkRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PartSupplierRepository partSupplierRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SynthesisExecutionService synthesisExecutionService;

    @Test
    void processRow_overwrite_true면_매핑된_part_속성들을_갱신하고_revision_snapshot을_남긴다() {
        Part existing = Part.create("P-001", "Old Name");
        existing.changeCategory("Old Category");
        existing.changeMaterial("Old Material");
        existing.changeUnit("EA");
        existing.changeDescription("Old Description");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(existing));

        MappingResultDto mapping = mappingWithPartFields();
        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "part_name", "New Name",
                "part_category", "New Category",
                "part_material", "AL6061",
                "part_unit", "SET",
                "part_description", "New Description"
        );

        ReflectionTestUtils.invokeMethod(
                synthesisExecutionService,
                "processRow",
                row,
                mapping,
                Map.of(),
                true,
                UUID.randomUUID()
        );

        assertEquals("New Name", existing.getName());
        assertEquals("New Category", existing.getCategory());
        assertEquals("AL6061", existing.getMaterial());
        assertEquals("SET", existing.getUnit());
        assertEquals("New Description", existing.getDescription());
        assertEquals("2", existing.getRevision());
        verify(partRevisionRepository).save(any(PartRevision.class));
        verify(partRepository, never()).save(any(Part.class));
    }

    @Test
    void processRow_overwrite_false면_기존값이_있을때_덮어쓰지_않는다() {
        Part existing = Part.create("P-001", "Old Name");
        existing.changeCategory("Old Category");
        existing.changeMaterial("Old Material");
        existing.changeUnit("EA");
        existing.changeDescription("Old Description");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(existing));

        MappingResultDto mapping = mappingWithPartFields();
        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "part_name", "New Name",
                "part_category", "New Category",
                "part_material", "AL6061",
                "part_unit", "SET",
                "part_description", "New Description"
        );

        ReflectionTestUtils.invokeMethod(
                synthesisExecutionService,
                "processRow",
                row,
                mapping,
                Map.of(),
                false,
                UUID.randomUUID()
        );

        assertEquals("Old Name", existing.getName());
        assertEquals("Old Category", existing.getCategory());
        assertEquals("Old Material", existing.getMaterial());
        assertEquals("EA", existing.getUnit());
        assertEquals("Old Description", existing.getDescription());
        assertEquals("1", existing.getRevision());
        verify(partRevisionRepository, never()).save(any(PartRevision.class));
    }

    @Test
    void processRow_신규_part_생성시_매핑된_속성을_채운다() {
        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.empty());

        MappingResultDto mapping = mappingWithPartFields();
        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "part_name", "New Name",
                "part_category", "New Category",
                "part_material", "AL6061",
                "part_unit", "SET",
                "part_description", "New Description"
        );

        ReflectionTestUtils.invokeMethod(
                synthesisExecutionService,
                "processRow",
                row,
                mapping,
                Map.of(),
                true,
                UUID.randomUUID()
        );

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(partCaptor.capture());
        verify(partRevisionRepository).save(any(PartRevision.class));

        Part created = partCaptor.getValue();
        assertEquals("P-001", created.getPartNumber());
        assertEquals("New Name", created.getName());
        assertEquals("New Category", created.getCategory());
        assertEquals("AL6061", created.getMaterial());
        assertEquals("SET", created.getUnit());
        assertEquals("New Description", created.getDescription());
    }

    private MappingResultDto mappingWithPartFields() {
        return new MappingResultDto(
                List.of(
                        new PropertyMappingDto("part_no", "part_number", null, PropertyDataType.STRING, 100, "", false),
                        new PropertyMappingDto("part_name", "name", null, PropertyDataType.STRING, 100, "", false),
                        new PropertyMappingDto("part_category", "category", null, PropertyDataType.STRING, 100, "", false),
                        new PropertyMappingDto("part_material", "material", null, PropertyDataType.STRING, 100, "", false),
                        new PropertyMappingDto("part_unit", "unit", null, PropertyDataType.STRING, 100, "", false),
                        new PropertyMappingDto("part_description", "description", null, PropertyDataType.STRING, 100, "", false)
                ),
                List.of()
        );
    }
}
