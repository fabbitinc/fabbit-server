package com.fabbitinc.server.application.synthesisv2.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mappingv2.dto.common.ExtendedPropertyMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.dto.common.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.RelationMappingV2Dto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingStatus;
import com.fabbitinc.server.domain.drawing.repository.DrawingRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import com.fabbitinc.server.domain.mappingv2.repository.MappingV2RevisionRepository;
import com.fabbitinc.server.domain.part.model.BomLink;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionActivityActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionActivitySourceType;
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
import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;
import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Job;
import com.fabbitinc.server.domain.synthesisv2.repository.SynthesisV2JobRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SynthesisV2ExecutionService {

    private final SynthesisV2JobRepository synthesisV2JobRepository;
    private final MappingV2RevisionRepository mappingV2RevisionRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final SpreadsheetParserSupport spreadsheetParserSupport;
    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final BomLinkRepository bomLinkRepository;
    private final DrawingRepository drawingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPartRepository projectPartRepository;
    private final SupplierRepository supplierRepository;
    private final PartSupplierRepository partSupplierRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runJob(UUID jobId, Map<String, String> rootContext, boolean overwrite) {
        SynthesisV2Job job = synthesisV2JobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != SynthesisJobStatus.PENDING) {
            return;
        }

        try {
            MappingV2Revision revision = mappingV2RevisionRepository.findFirstByRecordIdOrderByVersionDesc(job.getMappingId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "V2 매핑 리비전을 찾을 수 없습니다"));
            File file = fileRepository.findByIdAndDeletedAtIsNull(job.getFileId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "합성 파일을 찾을 수 없습니다"));
            MappingV2ResultDto mapping = parseMapping(revision.getMapping());

            byte[] content = storagePort.getObject(file.getFileKey());
            SpreadsheetParserSupport.ParsedSheet parsed = spreadsheetParserSupport.parse(
                    content,
                    file.getOriginalName(),
                    revision.getSheetName(),
                    -1
            );

            List<Map<String, Object>> rows = parsed.rows();
            job.start(rows.size());

            if (rows.isEmpty()) {
                job.complete(0, 0, 0, "[]");
                return;
            }

            int processedRows = 0;
            int nodesCreated = 0;
            int relationshipsCreated = 0;
            List<String> errors = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                processedRows++;
                try {
                    RowProcessResult result = processRow(row, mapping, rootContext, overwrite, file, job.getId());
                    nodesCreated += result.nodesCreated();
                    relationshipsCreated += result.relationshipsCreated();
                } catch (Exception ex) {
                    String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    errors.add("행 " + processedRows + ": " + message);
                }
            }

            job.complete(processedRows, nodesCreated, relationshipsCreated, serializeErrors(errors));
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            job.fail(serializeErrors(List.of(message)));
        }
    }

    private RowProcessResult processRow(
            Map<String, Object> row,
            MappingV2ResultDto mapping,
            Map<String, String> rootContext,
            boolean overwrite,
            File sourceFile,
            UUID jobId
    ) {
        int nodesCreated = 0;
        int relationshipsCreated = 0;
        Map<String, ResolvedNode> resolvedNodes = new LinkedHashMap<>();

        for (NodeMappingV2Dto node : mapping.nodes()) {
            ResolvedNode resolved = resolveNode(row, node, rootContext, overwrite, sourceFile, jobId);
            if (resolved == null) {
                continue;
            }
            resolvedNodes.put(node.nodeId(), resolved);
            if (resolved.created()) {
                nodesCreated++;
            }
        }

        for (RelationMappingV2Dto relation : mapping.relations()) {
            ResolvedNode fromNode = resolvedNodes.get(relation.fromNodeId());
            ResolvedNode toNode = resolvedNodes.get(relation.toNodeId());
            if (fromNode == null || toNode == null) {
                continue;
            }

            if (relation.relType() == RelationshipType.CONSISTS_OF
                    && "Part".equals(fromNode.label())
                    && "Part".equals(toNode.label())) {
                int quantity = resolveRelationIntegerProperty(row, relation, "quantity", 1);
                Map<String, Object> extendedProperties = resolveRelationProperties(row, relation, "quantity");
                if (upsertBomLink(fromNode.part(), toNode.part(), quantity, extendedProperties, overwrite)) {
                    relationshipsCreated++;
                }
                continue;
            }

            if (relation.relType() == RelationshipType.SUPPLIED_BY
                    && "Part".equals(fromNode.label())
                    && "Supplier".equals(toNode.label())) {
                Double unitCost = resolveRelationDoubleProperty(row, relation, "unit_cost");
                Map<String, Object> extendedProperties = resolveRelationProperties(row, relation, "unit_cost");
                if (upsertPartSupplier(fromNode.part(), toNode.supplier(), unitCost, extendedProperties, overwrite)) {
                    relationshipsCreated++;
                }
                continue;
            }

            if (relation.relType() == RelationshipType.DEFINED_BY
                    && "Part".equals(fromNode.label())
                    && "Drawing".equals(toNode.label())) {
                if (linkPartToDrawing(fromNode.part(), toNode.drawing())) {
                    relationshipsCreated++;
                }
                continue;
            }

            if (relation.relType() == RelationshipType.HAS_ITEM
                    && "Project".equals(fromNode.label())
                    && "Part".equals(toNode.label())) {
                if (linkProjectPart(fromNode.project(), toNode.part())) {
                    relationshipsCreated++;
                }
            }
        }

        return new RowProcessResult(nodesCreated, relationshipsCreated);
    }

    private ResolvedNode resolveNode(
            Map<String, Object> row,
            NodeMappingV2Dto node,
            Map<String, String> rootContext,
            boolean overwrite,
            File sourceFile,
            UUID jobId
    ) {
        if ("Part".equals(node.label())) {
            PartNodeValues values = resolvePartNodeValues(row, node, rootContext);
            if (values.partNumber() == null) {
                return null;
            }
            UpsertPartResult result = upsertPart(values, overwrite, jobId);
            return ResolvedNode.part(result.part(), result.created());
        }

        if ("Supplier".equals(node.label())) {
            SupplierNodeValues values = resolveSupplierNodeValues(row, node, rootContext);
            if (values.companyName() == null) {
                return null;
            }
            UpsertSupplierResult result = upsertSupplier(values, overwrite);
            return ResolvedNode.supplier(result.supplier(), result.created());
        }

        if ("Drawing".equals(node.label())) {
            DrawingNodeValues values = resolveDrawingNodeValues(row, node, rootContext);
            if (values.drawingNumber() == null) {
                return null;
            }
            UpsertDrawingResult result = upsertDrawing(values, overwrite);
            return ResolvedNode.drawing(result.drawing(), result.created());
        }

        if ("Project".equals(node.label())) {
            ProjectNodeValues values = resolveProjectNodeValues(row, node, rootContext, sourceFile);
            if (values == null || values.name() == null) {
                return null;
            }
            UpsertProjectResult result = upsertProject(values, overwrite);
            return ResolvedNode.project(result.project(), result.created());
        }

        return null;
    }

    private PartNodeValues resolvePartNodeValues(
            Map<String, Object> row,
            NodeMappingV2Dto node,
            Map<String, String> rootContext
    ) {
        String partNumber = resolveNodeTextProperty(row, node, "part_number");
        if (partNumber == null) {
            partNumber = resolveRootContextValue(rootContext, "Part");
        }

        return new PartNodeValues(
                partNumber,
                resolveNodeTextProperty(row, node, "name"),
                resolveNodeTextProperty(row, node, "category"),
                resolveNodeTextProperty(row, node, "material"),
                resolveNodeTextProperty(row, node, "unit"),
                resolveNodeTextProperty(row, node, "description"),
                resolveNodeBooleanProperty(row, node, "is_phantom"),
                PartLifecycleState.from(resolveNodeTextProperty(row, node, "lifecycle_state")),
                resolveNodeIntegerProperty(row, node, "lead_time_days"),
                resolveNodeExtendedProperties(row, node)
        );
    }

    private SupplierNodeValues resolveSupplierNodeValues(
            Map<String, Object> row,
            NodeMappingV2Dto node,
            Map<String, String> rootContext
    ) {
        String companyName = resolveNodeTextProperty(row, node, "company_name");
        if (companyName == null) {
            companyName = resolveRootContextValue(rootContext, "Supplier");
        }

        return new SupplierNodeValues(
                companyName,
                resolveNodeTextProperty(row, node, "code"),
                resolveNodeTextProperty(row, node, "country"),
                resolveNodeTextProperty(row, node, "contact_info"),
                resolveNodeExtendedProperties(row, node)
        );
    }

    private DrawingNodeValues resolveDrawingNodeValues(
            Map<String, Object> row,
            NodeMappingV2Dto node,
            Map<String, String> rootContext
    ) {
        String drawingNumber = resolveNodeTextProperty(row, node, "drawing_number");
        if (drawingNumber == null) {
            drawingNumber = resolveRootContextValue(rootContext, "Drawing");
        }
        String name = resolveNodeTextProperty(row, node, "name");
        if (name == null) {
            name = drawingNumber;
        }

        return new DrawingNodeValues(
                drawingNumber,
                name,
                resolveNodeTextProperty(row, node, "version"),
                resolveDrawingStatus(resolveNodeTextProperty(row, node, "status")),
                resolveNodeTextProperty(row, node, "file_path")
        );
    }

    private ProjectNodeValues resolveProjectNodeValues(
            Map<String, Object> row,
            NodeMappingV2Dto node,
            Map<String, String> rootContext,
            File sourceFile
    ) {
        String name = resolveNodeTextProperty(row, node, "name");
        if (name != null) {
            return new ProjectNodeValues(name, null, null);
        }

        String rootProjectName = resolveRootContextValue(rootContext, "Project");
        if (rootProjectName != null) {
            return new ProjectNodeValues(rootProjectName, null, null);
        }

        if (sourceFile == null || !"project".equals(sourceFile.getOwnerType()) || sourceFile.getOwnerId() == null) {
            return null;
        }

        Project ownerProject = projectRepository.findByIdAndDeletedFalse(sourceFile.getOwnerId()).orElse(null);
        if (ownerProject == null) {
            return null;
        }

        return new ProjectNodeValues(ownerProject.getName(), ownerProject.getDescription(), ownerProject);
    }

    private Map<String, Object> resolveNodeExtendedProperties(
            Map<String, Object> row,
            NodeMappingV2Dto node
    ) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (ExtendedPropertyMappingV2Dto property : node.extendedProperties()) {
            if (property.generatedKey() == null || property.generatedKey().isBlank()) {
                continue;
            }
            Object value = castValue(row.get(property.sourceColumn()), property.dataType());
            if (value == null) {
                continue;
            }
            properties.put(property.generatedKey(), value);
        }
        return properties;
    }

    private int resolveRelationIntegerProperty(
            Map<String, Object> row,
            RelationMappingV2Dto relation,
            String propertyName,
            int defaultValue
    ) {
        String column = relation.propertyColumns().get(propertyName);
        if (column == null || column.isBlank()) {
            return defaultValue;
        }
        PropertyDataType dataType = relation.propertyColumnTypes().getOrDefault(propertyName, PropertyDataType.INTEGER);
        Integer value = toInteger(castValue(row.get(column), dataType));
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private Double resolveRelationDoubleProperty(
            Map<String, Object> row,
            RelationMappingV2Dto relation,
            String propertyName
    ) {
        String column = relation.propertyColumns().get(propertyName);
        if (column == null || column.isBlank()) {
            return null;
        }
        PropertyDataType dataType = relation.propertyColumnTypes().getOrDefault(propertyName, PropertyDataType.FLOAT);
        return toDouble(castValue(row.get(column), dataType));
    }

    private Map<String, Object> resolveRelationProperties(
            Map<String, Object> row,
            RelationMappingV2Dto relation,
            String ignoredPropertyName
    ) {
        Map<String, Object> properties = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : relation.propertyColumns().entrySet()) {
            String propertyName = entry.getKey();
            if (propertyName == null || propertyName.isBlank() || propertyName.equals(ignoredPropertyName)) {
                continue;
            }

            PropertyDataType dataType = relation.propertyColumnTypes().getOrDefault(propertyName, PropertyDataType.STRING);
            Object value = castValue(row.get(entry.getValue()), dataType);
            if (value == null) {
                continue;
            }
            properties.put(propertyName, value);
        }

        for (ExtendedPropertyMappingV2Dto property : relation.extendedProperties()) {
            if (property.generatedKey() == null || property.generatedKey().isBlank()) {
                continue;
            }
            Object value = castValue(row.get(property.sourceColumn()), property.dataType());
            if (value == null) {
                continue;
            }
            properties.put(property.generatedKey(), value);
        }

        return properties;
    }

    private UpsertPartResult upsertPart(PartNodeValues values, boolean overwrite, UUID jobId) {
        Part existing = partRepository.findByPartNumber(values.partNumber()).orElse(null);
        if (existing != null) {
            PartRevision revision = findOrCreateWorkingRevision(existing, values.name());
            boolean changed = false;
            if (shouldApplyString(values.name(), revision.getName(), overwrite)) {
                revision.changeName(values.name());
                changed = true;
            }
            if (shouldApplyString(values.category(), revision.getCategory(), overwrite)) {
                revision.changeCategory(values.category());
                changed = true;
            }
            if (shouldApplyString(values.material(), revision.getMaterial(), overwrite)) {
                revision.changeMaterial(values.material());
                changed = true;
            }
            if (shouldApplyString(values.unit(), revision.getUnit(), overwrite)) {
                revision.changeUnit(values.unit());
                changed = true;
            }
            if (shouldApplyString(values.description(), revision.getDescription(), overwrite)) {
                revision.changeDescription(values.description());
                changed = true;
            }
            if (shouldApplyObject(values.phantom(), revision.getPhantom(), overwrite)) {
                applyPhantom(revision, values.phantom());
                changed = true;
            }
            if (shouldApplyObject(values.lifecycleState(), existing.getLifecycleState(), overwrite)) {
                applyLifecycleState(existing, values.lifecycleState());
                changed = true;
            }
            if (shouldApplyObject(values.leadTimeDays(), revision.getLeadTimeDays(), overwrite)) {
                revision.changeLeadTimeDays(values.leadTimeDays());
                changed = true;
            }

            MergedPropertiesResult mergedProperties = mergeExtendedProperties(
                    revision.getExtendedProperties(),
                    values.extendedProperties(),
                    overwrite
            );
            if (mergedProperties.changed()) {
                revision.changeExtendedProperties(mergedProperties.serialized());
                changed = true;
            }

            if (changed) {
                recordSynthesisImport(revision, jobId);
                partRevisionRepository.save(revision);
            }
            return new UpsertPartResult(existing, false);
        }

        Part created = Part.create(values.partNumber());
        if (values.lifecycleState() != null) {
            created.changeLifecycleState(values.lifecycleState());
        }
        partRepository.save(created);
        PartRevision revision = PartRevision.createInitial(created, "1", values.name());
        if (values.category() != null) {
            revision.changeCategory(values.category());
        }
        if (values.material() != null) {
            revision.changeMaterial(values.material());
        }
        if (values.unit() != null) {
            revision.changeUnit(values.unit());
        }
        if (values.description() != null) {
            revision.changeDescription(values.description());
        }
        if (values.phantom() != null) {
            applyPhantom(revision, values.phantom());
        }
        if (values.leadTimeDays() != null) {
            revision.changeLeadTimeDays(values.leadTimeDays());
        }
        if (!values.extendedProperties().isEmpty()) {
            revision.changeExtendedProperties(serializeProperties(values.extendedProperties()));
        }
        recordSynthesisImport(revision, jobId);
        partRevisionRepository.save(revision);
        return new UpsertPartResult(created, true);
    }

    private PartRevision findOrCreateWorkingRevision(Part part, String name) {
        PartRevision revision = resolveCurrentRevision(part);
        if (revision != null) {
            return revision;
        }
        PartRevision created = PartRevision.createInitial(part, "1", name);
        return partRevisionRepository.save(created);
    }

    private PartRevision resolveCurrentRevision(Part part) {
        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(part.getId());
        if (revisions.isEmpty()) {
            return null;
        }
        if (part.getCurrentReleasedRevisionId() != null) {
            for (PartRevision revision : revisions) {
                if (part.getCurrentReleasedRevisionId().equals(revision.getId())) {
                    return revision;
                }
            }
        }
        if (part.getCurrentApprovedRevisionId() != null) {
            for (PartRevision revision : revisions) {
                if (part.getCurrentApprovedRevisionId().equals(revision.getId())) {
                    return revision;
                }
            }
        }
        return revisions.get(0);
    }

    private void recordSynthesisImport(PartRevision revision, UUID jobId) {
        revision.recordActivity(
                null,
                PartRevisionActivityActionType.IMPORTED,
                PartRevisionActivitySourceType.SYNTHESIS,
                jobId,
                "{}"
        );
    }

    private UpsertSupplierResult upsertSupplier(SupplierNodeValues values, boolean overwrite) {
        Supplier existing = supplierRepository.findByCompanyName(values.companyName()).orElse(null);
        if (existing != null) {
            boolean changed = false;
            if (shouldApplyString(values.code(), existing.getCode(), overwrite)) {
                existing.changeCode(values.code());
                changed = true;
            }
            if (shouldApplyString(values.country(), existing.getCountry(), overwrite)) {
                existing.changeCountry(values.country());
                changed = true;
            }
            if (shouldApplyString(values.contactInfo(), existing.getContactInfo(), overwrite)) {
                existing.changeContactInfo(values.contactInfo());
                changed = true;
            }

            MergedPropertiesResult mergedProperties = mergeExtendedProperties(
                    existing.getExtendedProperties(),
                    values.extendedProperties(),
                    overwrite
            );
            if (mergedProperties.changed()) {
                existing.changeExtendedProperties(mergedProperties.serialized());
                changed = true;
            }
            return new UpsertSupplierResult(existing, false, changed);
        }

        Supplier created = Supplier.create(
                values.companyName(),
                values.code(),
                values.country(),
                values.contactInfo(),
                serializeProperties(values.extendedProperties())
        );
        supplierRepository.save(created);
        return new UpsertSupplierResult(created, true, true);
    }

    private UpsertDrawingResult upsertDrawing(DrawingNodeValues values, boolean overwrite) {
        Drawing existing = drawingRepository.findByDrawingNumberAndDeletedAtIsNull(values.drawingNumber()).orElse(null);
        if (existing != null) {
            if (shouldApplyString(values.name(), existing.getName(), overwrite)) {
                existing.changeName(values.name());
            }
            if (shouldApplyString(values.version(), existing.getVersion(), overwrite)) {
                existing.changeVersion(values.version());
            }
            if (shouldApplyDrawingStatus(values.status(), existing.getStatus(), overwrite)) {
                existing.changeStatus(values.status());
            }
            if (shouldApplyString(values.originalFileKey(), existing.getOriginalFileKey(), overwrite)) {
                existing.changeOriginalFileKey(values.originalFileKey());
            }
            return new UpsertDrawingResult(existing, false);
        }

        Drawing created = Drawing.create(values.drawingNumber(), values.name());
        if (values.version() != null) {
            created.changeVersion(values.version());
        }
        if (values.status() != null) {
            created.changeStatus(values.status());
        }
        if (values.originalFileKey() != null) {
            created.changeOriginalFileKey(values.originalFileKey());
        }
        drawingRepository.save(created);
        return new UpsertDrawingResult(created, true);
    }

    private UpsertProjectResult upsertProject(ProjectNodeValues values, boolean overwrite) {
        if (values.existingProject() != null) {
            return new UpsertProjectResult(values.existingProject(), false);
        }

        Project existing = projectRepository.findByNameAndDeletedFalse(values.name()).orElse(null);
        if (existing != null) {
            if (shouldApplyString(values.description(), existing.getDescription(), overwrite)) {
                existing.changeDescription(values.description());
            }
            return new UpsertProjectResult(existing, false);
        }

        Project created = Project.create(values.name(), values.description());
        projectRepository.save(created);
        return new UpsertProjectResult(created, true);
    }

    private boolean upsertBomLink(
            Part parent,
            Part child,
            int quantity,
            Map<String, Object> extendedProperties,
            boolean overwrite
    ) {
        BomLink existing = bomLinkRepository.findByParentPartIdAndChildPartId(parent.getId(), child.getId()).orElse(null);
        if (existing != null) {
            boolean changed = false;
            if (overwrite && existing.getQuantity() != quantity) {
                existing.changeQuantity(quantity);
                changed = true;
            }
            MergedPropertiesResult mergedProperties = mergeExtendedProperties(
                    existing.getExtendedProperties(),
                    extendedProperties,
                    overwrite
            );
            if (mergedProperties.changed()) {
                existing.changeExtendedProperties(mergedProperties.serialized());
                changed = true;
            }
            return changed;
        }

        bomLinkRepository.save(BomLink.connect(
                parent.getId(),
                child.getId(),
                quantity,
                serializeProperties(extendedProperties)
        ));
        return true;
    }

    private boolean upsertPartSupplier(
            Part part,
            Supplier supplier,
            Double unitCost,
            Map<String, Object> extendedProperties,
            boolean overwrite
    ) {
        PartSupplier existing = partSupplierRepository.findByPartIdAndSupplierId(part.getId(), supplier.getId()).orElse(null);
        if (existing != null) {
            boolean changed = false;
            if (shouldApplyDouble(unitCost, existing.getUnitCost(), overwrite)) {
                existing.changeUnitCost(unitCost);
                changed = true;
            }
            MergedPropertiesResult mergedProperties = mergeExtendedProperties(
                    existing.getExtendedProperties(),
                    extendedProperties,
                    overwrite
            );
            if (mergedProperties.changed()) {
                existing.changeExtendedProperties(mergedProperties.serialized());
                changed = true;
            }
            return changed;
        }

        partSupplierRepository.save(PartSupplier.link(
                part.getId(),
                supplier.getId(),
                unitCost,
                serializeProperties(extendedProperties)
        ));
        return true;
    }

    private boolean linkPartToDrawing(Part part, Drawing drawing) {
        if (part.getId().equals(drawing.getPartId())) {
            return false;
        }
        drawing.assignPart(part.getId());
        return true;
    }

    private boolean linkProjectPart(Project project, Part part) {
        ProjectPart existing = projectPartRepository.findByProjectIdAndPartId(project.getId(), part.getId()).orElse(null);
        if (existing != null) {
            return false;
        }
        projectPartRepository.save(project.linkPart(part.getId()));
        return true;
    }

    private MappingV2ResultDto parseMapping(String rawMapping) {
        if (rawMapping == null || rawMapping.isBlank()) {
            return new MappingV2ResultDto(List.of(), List.of());
        }
        try {
            return objectMapper.readValue(rawMapping, MappingV2ResultDto.class);
        } catch (JacksonException ex) {
            return new MappingV2ResultDto(List.of(), List.of());
        }
    }

    private String resolveNodeTextProperty(Map<String, Object> row, NodeMappingV2Dto node, String propertyName) {
        String column = node.propertyColumns().get(propertyName);
        if (column == null || column.isBlank()) {
            return null;
        }
        return normalizeText(castValue(row.get(column), PropertyDataType.STRING));
    }

    private Integer resolveNodeIntegerProperty(Map<String, Object> row, NodeMappingV2Dto node, String propertyName) {
        String column = node.propertyColumns().get(propertyName);
        if (column == null || column.isBlank()) {
            return null;
        }
        return toInteger(castValue(row.get(column), PropertyDataType.INTEGER));
    }

    private Boolean resolveNodeBooleanProperty(Map<String, Object> row, NodeMappingV2Dto node, String propertyName) {
        String column = node.propertyColumns().get(propertyName);
        if (column == null || column.isBlank()) {
            return null;
        }
        Object value = castValue(row.get(column), PropertyDataType.BOOLEAN);
        return value instanceof Boolean bool ? bool : null;
    }

    private DrawingStatus resolveDrawingStatus(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return DrawingStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveRootContextValue(Map<String, String> rootContext, String label) {
        if (rootContext == null || rootContext.isEmpty()) {
            return null;
        }
        return normalizeText(rootContext.get(label));
    }

    private void applyPhantom(PartRevision revision, Boolean phantom) {
        if (phantom == null) {
            revision.clearPhantomFlag();
            return;
        }
        if (phantom) {
            revision.markPhantom();
            return;
        }
        revision.markReal();
    }

    private void applyLifecycleState(Part part, PartLifecycleState lifecycleState) {
        if (lifecycleState == null) {
            part.clearLifecycleState();
            return;
        }
        part.changeLifecycleState(lifecycleState);
    }

    private boolean shouldApplyString(String incoming, String current, boolean overwrite) {
        if (incoming == null) {
            return false;
        }
        if (!overwrite && current != null && !current.isBlank()) {
            return false;
        }
        return !incoming.equals(current);
    }

    private boolean shouldApplyObject(Object incoming, Object current, boolean overwrite) {
        if (incoming == null) {
            return false;
        }
        if (!overwrite && current != null) {
            return false;
        }
        return !Objects.equals(incoming, current);
    }

    private boolean shouldApplyDrawingStatus(DrawingStatus incoming, DrawingStatus current, boolean overwrite) {
        if (incoming == null) {
            return false;
        }
        if (!overwrite && current != null) {
            return false;
        }
        return incoming != current;
    }

    private boolean shouldApplyDouble(Double incoming, Double current, boolean overwrite) {
        if (incoming == null) {
            return false;
        }
        if (!overwrite && current != null) {
            return false;
        }
        return !incoming.equals(current);
    }

    private String serializeErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JacksonException ex) {
            return "[]";
        }
    }

    private String serializeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JacksonException ex) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseProperties(String raw) {
        if (raw == null || raw.isBlank() || "{}".equals(raw.trim())) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            if (parsed == null || parsed.isEmpty()) {
                return new LinkedHashMap<>();
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                normalized.put(entry.getKey(), entry.getValue());
            }
            return normalized;
        } catch (JacksonException ex) {
            return new LinkedHashMap<>();
        }
    }

    private MergedPropertiesResult mergeExtendedProperties(
            String currentRaw,
            Map<String, Object> incoming,
            boolean overwrite
    ) {
        if (incoming == null || incoming.isEmpty()) {
            return new MergedPropertiesResult("{}", false);
        }

        Map<String, Object> merged = parseProperties(currentRaw);
        boolean changed = false;
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            Object currentValue = merged.get(entry.getKey());
            if (!overwrite && currentValue != null) {
                continue;
            }
            if (Objects.equals(currentValue, entry.getValue())) {
                continue;
            }
            merged.put(entry.getKey(), entry.getValue());
            changed = true;
        }
        return new MergedPropertiesResult(serializeProperties(merged), changed);
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private Object castValue(Object raw, PropertyDataType dataType) {
        if (raw == null) {
            return null;
        }
        String text = raw.toString().trim();
        if (text.isBlank()) {
            return null;
        }
        PropertyDataType normalizedType = dataType == null ? PropertyDataType.STRING : dataType;
        return switch (normalizedType) {
            case INTEGER -> toInteger(text);
            case FLOAT -> toDouble(text);
            case BOOLEAN -> toBoolean(text);
            default -> text;
        };
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean toBoolean(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (List.of("true", "1", "yes", "y").contains(normalized)) {
            return true;
        }
        if (List.of("false", "0", "no", "n").contains(normalized)) {
            return false;
        }
        return null;
    }

    private record RowProcessResult(
            int nodesCreated,
            int relationshipsCreated
    ) {
    }

    private record UpsertPartResult(
            Part part,
            boolean created
    ) {
    }

    private record UpsertSupplierResult(
            Supplier supplier,
            boolean created,
            boolean changed
    ) {
    }

    private record UpsertDrawingResult(
            Drawing drawing,
            boolean created
    ) {
    }

    private record UpsertProjectResult(
            Project project,
            boolean created
    ) {
    }

    private record PartNodeValues(
            String partNumber,
            String name,
            String category,
            String material,
            String unit,
            String description,
            Boolean phantom,
            PartLifecycleState lifecycleState,
            Integer leadTimeDays,
            Map<String, Object> extendedProperties
    ) {
    }

    private record SupplierNodeValues(
            String companyName,
            String code,
            String country,
            String contactInfo,
            Map<String, Object> extendedProperties
    ) {
    }

    private record DrawingNodeValues(
            String drawingNumber,
            String name,
            String version,
            DrawingStatus status,
            String originalFileKey
    ) {
    }

    private record ProjectNodeValues(
            String name,
            String description,
            Project existingProject
    ) {
    }

    private record MergedPropertiesResult(
            String serialized,
            boolean changed
    ) {
    }

    private record ResolvedNode(
            String label,
            Part part,
            Supplier supplier,
            Drawing drawing,
            Project project,
            boolean created
    ) {
        private static ResolvedNode part(Part part, boolean created) {
            return new ResolvedNode("Part", part, null, null, null, created);
        }

        private static ResolvedNode supplier(Supplier supplier, boolean created) {
            return new ResolvedNode("Supplier", null, supplier, null, null, created);
        }

        private static ResolvedNode drawing(Drawing drawing, boolean created) {
            return new ResolvedNode("Drawing", null, null, drawing, null, created);
        }

        private static ResolvedNode project(Project project, boolean created) {
            return new ResolvedNode("Project", null, null, null, project, created);
        }
    }
}
