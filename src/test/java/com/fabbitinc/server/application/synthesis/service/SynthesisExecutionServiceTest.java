package com.fabbitinc.server.application.synthesis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.common.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.dto.common.RelationMappingDto;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingStatus;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
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
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
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
    private DrawingRepository drawingRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectPartRepository projectPartRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PartSupplierRepository partSupplierRepository;
    private SynthesisExecutionService synthesisExecutionService;

    @BeforeEach
    void setUp() {
        synthesisExecutionService = new SynthesisExecutionService(
                synthesisJobRepository,
                mappingRevisionRepository,
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
                new ObjectMapper()
        );
    }

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
                null,
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
                null,
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
                null,
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

    @Test
    void processRow_consistsOf_관계_확장속성을_BOM에_저장한다() {
        Part child = Part.create("C-001", "Child");
        Part parent = Part.create("P-001", "Parent");

        when(partRepository.findByPartNumber("C-001")).thenReturn(Optional.of(child));
        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(parent));
        when(bomLinkRepository.findByParentPartIdAndChildPartId(parent.getId(), child.getId())).thenReturn(Optional.empty());

        MappingResultDto mapping = new MappingResultDto(
                List.of(
                        new PropertyMappingDto("part_no", "part_number", null, PropertyDataType.STRING, 100, "", false)
                ),
                List.of(
                        new RelationMappingDto(
                                RelationshipType.CONSISTS_OF,
                                "Part",
                                Map.of("part_number", "parent_no"),
                                Map.of("quantity", "qty", "sequence", "seq"),
                                Map.of("quantity", PropertyDataType.INTEGER, "sequence", PropertyDataType.INTEGER),
                                100,
                                ""
                        )
                )
        );

        Map<String, Object> row = Map.of(
                "part_no", "C-001",
                "parent_no", "P-001",
                "qty", "2",
                "seq", "10"
        );

        invokeProcessRow(row, mapping, Map.of(), true, null);

        ArgumentCaptor<BomLink> captor = ArgumentCaptor.forClass(BomLink.class);
        verify(bomLinkRepository).save(captor.capture());
        BomLink saved = captor.getValue();
        assertEquals(2, saved.getQuantity());
        assertEquals("{\"sequence\":10}", saved.getExtendedProperties());
    }

    @Test
    void processRow_suppliedBy_관계_확장속성을_PartSupplier에_저장한다() {
        Part part = Part.create("P-001", "Part");
        Supplier supplier = Supplier.create("ACME", null, null, null, "{}");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(supplierRepository.findByCompanyName("ACME")).thenReturn(Optional.of(supplier));
        when(partSupplierRepository.findByPartIdAndSupplierId(part.getId(), supplier.getId())).thenReturn(Optional.empty());

        MappingResultDto mapping = new MappingResultDto(
                List.of(
                        new PropertyMappingDto("part_no", "part_number", null, PropertyDataType.STRING, 100, "", false)
                ),
                List.of(
                        new RelationMappingDto(
                                RelationshipType.SUPPLIED_BY,
                                "Supplier",
                                Map.of("company_name", "supplier_name"),
                                Map.of("unit_cost", "price", "_ext_moq", "moq"),
                                Map.of("unit_cost", PropertyDataType.FLOAT, "_ext_moq", PropertyDataType.INTEGER),
                                100,
                                ""
                        )
                )
        );

        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "supplier_name", "ACME",
                "price", "12.5",
                "moq", "100"
        );

        invokeProcessRow(row, mapping, Map.of(), true, null);

        ArgumentCaptor<PartSupplier> captor = ArgumentCaptor.forClass(PartSupplier.class);
        verify(partSupplierRepository).save(captor.capture());
        PartSupplier saved = captor.getValue();
        assertEquals(12.5, saved.getUnitCost());
        assertEquals("{\"_ext_moq\":100}", saved.getExtendedProperties());
    }

    @Test
    void processRow_definedBy_도면을_upsert하고_part에_연결한다() {
        Part part = Part.create("P-001", "Part");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(drawingRepository.findByDrawingNumberAndDeletedAtIsNull("D-001")).thenReturn(Optional.empty());

        MappingResultDto mapping = new MappingResultDto(
                List.of(
                        new PropertyMappingDto("part_no", "part_number", null, PropertyDataType.STRING, 100, "", false)
                ),
                List.of(
                        new RelationMappingDto(
                                RelationshipType.DEFINED_BY,
                                "Drawing",
                                Map.of(
                                        "drawing_number", "drawing_no",
                                        "name", "drawing_name",
                                        "version", "drawing_version",
                                        "status", "drawing_status"
                                ),
                                Map.of(),
                                Map.of(),
                                100,
                                ""
                        )
                )
        );

        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "drawing_no", "D-001",
                "drawing_name", "Main Drawing",
                "drawing_version", "A",
                "drawing_status", "released"
        );

        invokeProcessRow(row, mapping, Map.of(), true, null);

        ArgumentCaptor<Drawing> captor = ArgumentCaptor.forClass(Drawing.class);
        verify(drawingRepository).save(captor.capture());
        Drawing saved = captor.getValue();
        assertEquals("D-001", saved.getDrawingNumber());
        assertEquals("Main Drawing", saved.getName());
        assertEquals("A", saved.getVersion());
        assertEquals(DrawingStatus.RELEASED, saved.getStatus());
        assertEquals(saved.getId(), part.getDrawingId());
    }

    @Test
    void processRow_hasItem_프로젝트를_upsert하고_part를_연결한다() {
        Part part = Part.create("P-001", "Part");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(projectRepository.findByNameAndDeletedFalse("EV Motor Project")).thenReturn(Optional.empty());
        when(projectPartRepository.findByProjectIdAndPartId(any(), any())).thenReturn(Optional.empty());

        MappingResultDto mapping = new MappingResultDto(
                List.of(
                        new PropertyMappingDto("part_no", "part_number", null, PropertyDataType.STRING, 100, "", false)
                ),
                List.of(
                        new RelationMappingDto(
                                RelationshipType.HAS_ITEM,
                                "Project",
                                Map.of("name", "project_name"),
                                Map.of(),
                                Map.of(),
                                100,
                                ""
                        )
                )
        );

        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "project_name", "EV Motor Project"
        );

        invokeProcessRow(row, mapping, Map.of(), true, null);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        Project savedProject = projectCaptor.getValue();
        assertEquals("EV Motor Project", savedProject.getName());

        ArgumentCaptor<ProjectPart> projectPartCaptor = ArgumentCaptor.forClass(ProjectPart.class);
        verify(projectPartRepository).save(projectPartCaptor.capture());
        ProjectPart savedLink = projectPartCaptor.getValue();
        assertEquals(savedProject.getId(), savedLink.getProjectId());
        assertEquals(part.getId(), savedLink.getPartId());
    }

    @Test
    void processRow_hasItem_프로젝트_컬럼이_없으면_파일_소유_프로젝트를_사용한다() {
        Part part = Part.create("P-001", "Part");
        Project project = Project.create("Owned Project", null);
        File file = File.create("items.xlsx", "files/items.xlsx", "application/vnd.ms-excel", 100L);
        file.markUploaded();
        file.assignOwner("project", project.getId());

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(projectRepository.findByIdAndDeletedFalse(project.getId())).thenReturn(Optional.of(project));
        when(projectPartRepository.findByProjectIdAndPartId(project.getId(), part.getId())).thenReturn(Optional.empty());

        MappingResultDto mapping = new MappingResultDto(
                List.of(
                        new PropertyMappingDto("part_no", "part_number", null, PropertyDataType.STRING, 100, "", false)
                ),
                List.of(
                        new RelationMappingDto(
                                RelationshipType.HAS_ITEM,
                                "Project",
                                Map.of(),
                                Map.of(),
                                Map.of(),
                                100,
                                ""
                        )
                )
        );

        Map<String, Object> row = Map.of("part_no", "P-001");

        invokeProcessRow(row, mapping, Map.of(), true, file);

        verify(projectRepository, never()).save(any(Project.class));
        ArgumentCaptor<ProjectPart> captor = ArgumentCaptor.forClass(ProjectPart.class);
        verify(projectPartRepository).save(captor.capture());
        assertEquals(project.getId(), captor.getValue().getProjectId());
        assertEquals(part.getId(), captor.getValue().getPartId());
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

    private void invokeProcessRow(
            Map<String, Object> row,
            MappingResultDto mapping,
            Map<String, String> rootContext,
            boolean overwrite,
            File sourceFile
    ) {
        Object result = ReflectionTestUtils.invokeMethod(
                synthesisExecutionService,
                "processRow",
                row,
                mapping,
                rootContext,
                overwrite,
                sourceFile,
                UUID.randomUUID()
        );
        assertNotNull(result);
    }
}
