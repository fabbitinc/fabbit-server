package com.fabbitinc.server.application.synthesisv2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mappingv2.dto.common.ExtendedPropertyMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.dto.common.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.RelationMappingV2Dto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RevisionRepository;
import com.fabbitinc.server.domain.part.model.BomLink;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartSupplier;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.project.model.Project;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import com.fabbitinc.server.domain.synthesisv2.repository.SynthesisV2JobRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SynthesisV2ExecutionServiceTest {

    @Mock
    private SynthesisV2JobRepository synthesisV2JobRepository;
    @Mock
    private MappingV2RevisionRepository mappingV2RevisionRepository;
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
    private com.fabbitinc.server.domain.drawing.repository.DrawingRepository drawingRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectPartRepository projectPartRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PartSupplierRepository partSupplierRepository;

    private SynthesisV2ExecutionService synthesisV2ExecutionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        synthesisV2ExecutionService = new SynthesisV2ExecutionService(
                synthesisV2JobRepository,
                mappingV2RevisionRepository,
                fileRepository,
                storagePort,
                spreadsheetParserSupport,
                partRepository,
                partRevisionRepository,
                bomLinkRepository,
                drawingRepository,
                projectRepository,
                projectPartRepository,
                supplierRepository,
                partSupplierRepository,
                objectMapper
        );
    }

    @Test
    void processRow_nodes와_relations를_해석해_각_도메인에_저장한다() throws Exception {
        Part parent = Part.create("P-001", "Parent");

        when(partRepository.findByPartNumber("C-001")).thenReturn(Optional.empty());
        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(parent));
        when(supplierRepository.findByCompanyName("ACME")).thenReturn(Optional.empty());
        when(drawingRepository.findByDrawingNumberAndDeletedAtIsNull("D-001")).thenReturn(Optional.empty());
        when(projectRepository.findByNameAndDeletedFalse("Root Project")).thenReturn(Optional.empty());
        when(bomLinkRepository.findByParentPartIdAndChildPartId(any(), any())).thenReturn(Optional.empty());
        when(partSupplierRepository.findByPartIdAndSupplierId(any(), any())).thenReturn(Optional.empty());
        when(projectPartRepository.findByProjectIdAndPartId(any(), any())).thenReturn(Optional.empty());

        MappingV2ResultDto mapping = new MappingV2ResultDto(
                List.of(
                        new NodeMappingV2Dto(
                                "child_part",
                                "Part",
                                Map.of("part_number", "child_no", "name", "child_name"),
                                List.of(new ExtendedPropertyMappingV2Dto("child_note", "_ext_remark", PropertyDataType.STRING)),
                                95,
                                "child"
                        ),
                        new NodeMappingV2Dto(
                                "parent_part",
                                "Part",
                                Map.of("part_number", "parent_no", "name", "parent_name"),
                                List.of(),
                                95,
                                "parent"
                        ),
                        new NodeMappingV2Dto(
                                "supplier_main",
                                "Supplier",
                                Map.of("company_name", "supplier_name"),
                                List.of(new ExtendedPropertyMappingV2Dto("supplier_grade", "_ext_grade", PropertyDataType.STRING)),
                                95,
                                "supplier"
                        ),
                        new NodeMappingV2Dto(
                                "drawing_main",
                                "Drawing",
                                Map.of("drawing_number", "drawing_no", "name", "drawing_name"),
                                List.of(),
                                95,
                                "drawing"
                        ),
                        new NodeMappingV2Dto(
                                "project_root",
                                "Project",
                                Map.of(),
                                List.of(),
                                95,
                                "project"
                        )
                ),
                List.of(
                        new RelationMappingV2Dto(
                                "parent_part",
                                RelationshipType.CONSISTS_OF,
                                "child_part",
                                Map.of("quantity", "qty", "sequence", "seq"),
                                Map.of("quantity", PropertyDataType.INTEGER, "sequence", PropertyDataType.INTEGER),
                                List.of(new ExtendedPropertyMappingV2Dto("process", "_ext_process", PropertyDataType.STRING)),
                                90,
                                "bom"
                        ),
                        new RelationMappingV2Dto(
                                "child_part",
                                RelationshipType.SUPPLIED_BY,
                                "supplier_main",
                                Map.of("unit_cost", "price"),
                                Map.of("unit_cost", PropertyDataType.FLOAT),
                                List.of(new ExtendedPropertyMappingV2Dto("moq", "_ext_moq", PropertyDataType.INTEGER)),
                                90,
                                "supplier"
                        ),
                        new RelationMappingV2Dto(
                                "child_part",
                                RelationshipType.DEFINED_BY,
                                "drawing_main",
                                Map.of(),
                                Map.of(),
                                List.of(),
                                90,
                                "drawing"
                        ),
                        new RelationMappingV2Dto(
                                "project_root",
                                RelationshipType.HAS_ITEM,
                                "child_part",
                                Map.of(),
                                Map.of(),
                                List.of(),
                                90,
                                "project"
                        )
                )
        );

        Map<String, Object> row = Map.ofEntries(
                Map.entry("child_no", "C-001"),
                Map.entry("child_name", "Child"),
                Map.entry("child_note", "공용부품"),
                Map.entry("parent_no", "P-001"),
                Map.entry("parent_name", "Parent"),
                Map.entry("supplier_name", "ACME"),
                Map.entry("supplier_grade", "A"),
                Map.entry("drawing_no", "D-001"),
                Map.entry("drawing_name", "Main Drawing"),
                Map.entry("qty", "2"),
                Map.entry("seq", "10"),
                Map.entry("process", "weld"),
                Map.entry("price", "12.5"),
                Map.entry("moq", "100")
        );

        Object result = ReflectionTestUtils.invokeMethod(
                synthesisV2ExecutionService,
                "processRow",
                row,
                mapping,
                Map.of("Project", "Root Project"),
                true,
                (File) null,
                UUID.randomUUID()
        );
        assertNotNull(result);

        ArgumentCaptor<Part> partCaptor = ArgumentCaptor.forClass(Part.class);
        verify(partRepository).save(partCaptor.capture());
        Part createdPart = partCaptor.getValue();
        assertEquals("C-001", createdPart.getPartNumber());
        Map<?, ?> childExt = objectMapper.readValue(createdPart.getExtendedProperties(), Map.class);
        assertEquals("공용부품", childExt.get("_ext_remark"));
        verify(partRevisionRepository).save(any(PartRevision.class));

        ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplierCaptor.capture());
        Map<?, ?> supplierExt = objectMapper.readValue(supplierCaptor.getValue().getExtendedProperties(), Map.class);
        assertEquals("A", supplierExt.get("_ext_grade"));

        ArgumentCaptor<Drawing> drawingCaptor = ArgumentCaptor.forClass(Drawing.class);
        verify(drawingRepository).save(drawingCaptor.capture());
        Drawing drawing = drawingCaptor.getValue();
        assertEquals("D-001", drawing.getDrawingNumber());
        assertEquals(drawing.getId(), createdPart.getDrawingId());

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertEquals("Root Project", projectCaptor.getValue().getName());

        ArgumentCaptor<BomLink> bomCaptor = ArgumentCaptor.forClass(BomLink.class);
        verify(bomLinkRepository).save(bomCaptor.capture());
        assertEquals(2, bomCaptor.getValue().getQuantity());
        Map<?, ?> bomExt = objectMapper.readValue(bomCaptor.getValue().getExtendedProperties(), Map.class);
        assertEquals(10, ((Number) bomExt.get("sequence")).intValue());
        assertEquals("weld", bomExt.get("_ext_process"));

        ArgumentCaptor<PartSupplier> supplierLinkCaptor = ArgumentCaptor.forClass(PartSupplier.class);
        verify(partSupplierRepository).save(supplierLinkCaptor.capture());
        assertEquals(12.5, supplierLinkCaptor.getValue().getUnitCost());
        Map<?, ?> supplierLinkExt = objectMapper.readValue(supplierLinkCaptor.getValue().getExtendedProperties(), Map.class);
        assertEquals(100, ((Number) supplierLinkExt.get("_ext_moq")).intValue());

        verify(projectPartRepository).save(any(ProjectPart.class));
    }
}
