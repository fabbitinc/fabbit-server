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
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingStatus;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevisionActivityActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionActivitySourceType;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.model.PartSupplier;
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
import java.math.BigDecimal;
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
    private EngineeringBomItemRepository engineeringBomItemRepository;
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
                engineeringBomItemRepository,
                drawingRepository,
                projectRepository,
                projectPartRepository,
                supplierRepository,
                partSupplierRepository,
                new ObjectMapper()
        );
    }

    @Test
    void processRow_overwrite_true면_현재_revision을_갱신하고_import_activity를_남긴다() {
        Part existing = Part.create("P-001");
        PartRevision existingRevision = currentRevisionOf(existing, "1", "Old Name");
        existingRevision.changeCategory("Old Category");
        existingRevision.changeMaterial("Old Material");
        existingRevision.changeUnit("EA");
        existingRevision.changeDescription("Old Description");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(existing));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(existingRevision));
        when(partRevisionRepository.save(any(PartRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MappingResultDto mapping = mappingWithPartFields();
        Map<String, Object> row = Map.of(
                "part_no", "P-001",
                "part_name", "New Name",
                "part_category", "New Category",
                "part_material", "AL6061",
                "part_unit", "SET",
                "part_description", "New Description"
        );
        UUID jobId = UUID.randomUUID();

        ReflectionTestUtils.invokeMethod(
                synthesisExecutionService,
                "processRow",
                row,
                mapping,
                Map.of(),
                true,
                null,
                jobId
        );

        assertEquals("New Name", existingRevision.getName());
        assertEquals("New Category", existingRevision.getCategory());
        assertEquals("AL6061", existingRevision.getMaterial());
        assertEquals("SET", existingRevision.getUnit());
        assertEquals("New Description", existingRevision.getDescription());
        assertEquals(1, existingRevision.getActivities().size());
        assertEquals(PartRevisionActivityActionType.IMPORTED, existingRevision.getActivities().get(0).getActionType());
        assertEquals(PartRevisionActivitySourceType.SYNTHESIS, existingRevision.getActivities().get(0).getSourceType());
        assertEquals(jobId, existingRevision.getActivities().get(0).getSourceRefId());
        verify(partRevisionRepository).save(any(PartRevision.class));
        verify(partRepository, never()).save(any(Part.class));
    }

    @Test
    void processRow_overwrite_false면_기존_revision값을_덮어쓰지_않는다() {
        Part existing = Part.create("P-001");
        PartRevision existingRevision = currentRevisionOf(existing, "1", "Old Name");
        existingRevision.changeCategory("Old Category");
        existingRevision.changeMaterial("Old Material");
        existingRevision.changeUnit("EA");
        existingRevision.changeDescription("Old Description");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(existing));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(existing.getId()))
                .thenReturn(List.of(existingRevision));

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

        assertEquals("Old Name", existingRevision.getName());
        assertEquals("Old Category", existingRevision.getCategory());
        assertEquals("Old Material", existingRevision.getMaterial());
        assertEquals("EA", existingRevision.getUnit());
        assertEquals("Old Description", existingRevision.getDescription());
        assertEquals(0, existingRevision.getActivities().size());
        verify(partRevisionRepository, never()).save(any(PartRevision.class));
    }

    @Test
    void processRow_신규_part_생성시_매핑된_속성을_채운다() {
        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.empty());
        when(partRepository.save(any(Part.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partRevisionRepository.save(any(PartRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        ArgumentCaptor<PartRevision> revisionCaptor = ArgumentCaptor.forClass(PartRevision.class);
        verify(partRepository).save(partCaptor.capture());
        verify(partRevisionRepository).save(revisionCaptor.capture());

        Part created = partCaptor.getValue();
        PartRevision createdRevision = revisionCaptor.getValue();
        assertEquals("P-001", created.getPartNumber());
        assertEquals("P-001", createdRevision.getPartNumber());
        assertEquals("New Name", createdRevision.getName());
        assertEquals("New Category", createdRevision.getCategory());
        assertEquals("AL6061", createdRevision.getMaterial());
        assertEquals("SET", createdRevision.getUnit());
        assertEquals("New Description", createdRevision.getDescription());
        assertEquals(1, createdRevision.getActivities().size());
    }

    @Test
    void processRow_consistsOf_관계_확장속성을_BOM에_저장한다() {
        Part child = Part.create("C-001");
        Part parent = Part.create("P-001");
        PartRevision childRevision = currentRevisionOf(child, "1", "Child");
        PartRevision parentRevision = currentRevisionOf(parent, "1", "Parent");

        when(partRepository.findByPartNumber("C-001")).thenReturn(Optional.of(child));
        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(parent));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(child.getId()))
                .thenReturn(List.of(childRevision));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(parent.getId()))
                .thenReturn(List.of(parentRevision));
        when(engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(parentRevision.getId(), "10"))
                .thenReturn(Optional.empty());

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

        ArgumentCaptor<EngineeringBomItem> captor = ArgumentCaptor.forClass(EngineeringBomItem.class);
        verify(engineeringBomItemRepository).save(captor.capture());
        EngineeringBomItem saved = captor.getValue();
        assertEquals("10", saved.getLineNumber());
        assertEquals(0, new BigDecimal("2").compareTo(saved.getQuantity()));
        assertEquals("{}", saved.getExtendedProperties());
    }

    @Test
    void processRow_suppliedBy_관계_확장속성을_PartSupplier에_저장한다() {
        Part part = Part.create("P-001");
        Supplier supplier = Supplier.create("ACME", null, null, null, "{}");
        PartRevision partRevision = currentRevisionOf(part, "1", "Part");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId()))
                .thenReturn(List.of(partRevision));
        when(supplierRepository.findByCompanyName("ACME")).thenReturn(Optional.of(supplier));
        when(partSupplierRepository.findByPartRevisionIdAndSupplierId(partRevision.getId(), supplier.getId()))
                .thenReturn(Optional.empty());

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
        Part part = Part.create("P-001");

        PartRevision currentRevision = currentRevisionOf(part, "1", "Part");
        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId()))
                .thenReturn(List.of(currentRevision));
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
        assertEquals(currentRevision.getId(), saved.getPartRevisionId());
    }

    @Test
    void processRow_hasItem_프로젝트를_upsert하고_part를_연결한다() {
        Part part = Part.create("P-001");

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId()))
                .thenReturn(List.of(currentRevisionOf(part, "1", "Part")));
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
        Part part = Part.create("P-001");
        Project project = Project.create("Owned Project", null);
        File file = File.create("items.xlsx", "files/items.xlsx", "application/vnd.ms-excel", 100L);
        file.markUploaded();
        file.assignOwner("project", project.getId());

        when(partRepository.findByPartNumber("P-001")).thenReturn(Optional.of(part));
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId()))
                .thenReturn(List.of(currentRevisionOf(part, "1", "Part")));
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

    private PartRevision currentRevisionOf(Part part, String revisionCode, String name) {
        if (revisionCode == null) {
            return PartRevision.createInitialDraft(part, "D1", name);
        }
        return PartRevision.createOfficial(part, revisionCode, null, name, PartRevisionStatus.RELEASED);
    }
}
