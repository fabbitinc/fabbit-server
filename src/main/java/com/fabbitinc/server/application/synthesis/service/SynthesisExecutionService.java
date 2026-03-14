package com.fabbitinc.server.application.synthesis.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
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
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevisionActivityActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionActivitySourceType;
import com.fabbitinc.server.domain.part.model.PartRevision;
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
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SynthesisExecutionService {

    private final SynthesisJobRepository synthesisJobRepository;
    private final MappingRevisionRepository mappingRevisionRepository;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final SpreadsheetParserSupport spreadsheetParserSupport;
    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final DrawingRepository drawingRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPartRepository projectPartRepository;
    private final SupplierRepository supplierRepository;
    private final PartSupplierRepository partSupplierRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runJob(UUID jobId, Map<String, String> rootContext, boolean overwrite) {
        SynthesisJob job = synthesisJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        if (job.getStatus() != SynthesisJobStatus.PENDING) {
            return;
        }

        try {
            MappingRevision revision = mappingRevisionRepository.findFirstByRecordIdOrderByVersionDesc(job.getMappingId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "매핑 리비전을 찾을 수 없습니다"));
            File file = fileRepository.findByIdAndDeletedAtIsNull(job.getFileId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "합성 파일을 찾을 수 없습니다"));
            MappingResultDto mapping = parseMapping(revision.getMapping());

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
            MappingResultDto mapping,
            Map<String, String> rootContext,
            boolean overwrite,
            File sourceFile,
            UUID jobId
    ) {
        int nodesCreated = 0;
        int relationshipsCreated = 0;

        PartRowValues childValues = resolveChildPartValues(row, mapping.propertyMappings());
        if (childValues.partNumber() == null) {
            return new RowProcessResult(0, 0);
        }

        UpsertPartResult childResult = upsertPart(childValues, overwrite, jobId);
        if (childResult.created()) {
            nodesCreated++;
        }

        for (RelationMappingDto relation : mapping.relationMappings()) {
            if (isConsistsOfPartRelation(relation)) {
                String parentPartNumber = resolveParentPartNumber(row, relation, rootContext);
                if (parentPartNumber == null || parentPartNumber.equals(childValues.partNumber())) {
                    continue;
                }

                String parentName = resolveMappedText(row, relation.nodeColumns().get("name"), PropertyDataType.STRING);
                UpsertPartResult parentResult = upsertPart(
                        new PartRowValues(parentPartNumber, parentName, null, null, null, null),
                        overwrite,
                        jobId
                );
                if (parentResult.created()) {
                    nodesCreated++;
                }

                BigDecimal quantity = resolveQuantity(row, relation);
                String lineNumber = resolveBomLineNumber(row, relation);
                Map<String, Object> extendedProperties = resolveRelationExtendedProperties(
                        row,
                        relation,
                        "quantity",
                        "line_number",
                        "find_number",
                        "sequence"
                );
                if (upsertEngineeringBomItem(
                        parentResult.revision(),
                        childResult.revision(),
                        lineNumber,
                        quantity,
                        extendedProperties,
                        overwrite
                )) {
                    relationshipsCreated++;
                }
                continue;
            }

            if (isDefinedByDrawingRelation(relation)) {
                DrawingRowValues drawingValues = resolveDrawingValues(row, relation, rootContext);
                if (drawingValues == null || drawingValues.drawingNumber() == null) {
                    continue;
                }

                UpsertDrawingResult drawingResult = upsertDrawing(drawingValues, overwrite);
                if (drawingResult.created()) {
                    nodesCreated++;
                }

                if (linkPartRevisionToDrawing(childResult.revision(), drawingResult.drawing())) {
                    relationshipsCreated++;
                }
                continue;
            }

            if (isSuppliedBySupplierRelation(relation)) {
                String companyName = resolveSupplierName(row, relation, rootContext);
                if (companyName == null) {
                    continue;
                }

                UpsertSupplierResult supplierResult = upsertSupplier(companyName);
                if (supplierResult.created()) {
                    nodesCreated++;
                }

                Double unitCost = resolveUnitCost(row, relation);
                Map<String, Object> extendedProperties = resolveRelationExtendedProperties(row, relation, "unit_cost");
                if (upsertPartSupplier(childResult.revision(), supplierResult.supplier(), unitCost, extendedProperties, overwrite)) {
                    relationshipsCreated++;
                }
                continue;
            }

            if (isHasItemProjectRelation(relation)) {
                ProjectRowValues projectValues = resolveProjectValues(row, relation, rootContext, sourceFile);
                if (projectValues == null) {
                    continue;
                }

                UpsertProjectResult projectResult = upsertProject(projectValues);
                if (projectResult.created()) {
                    nodesCreated++;
                }

                if (linkProjectPart(projectResult.project(), childResult.part())) {
                    relationshipsCreated++;
                }
            }
        }

        return new RowProcessResult(nodesCreated, relationshipsCreated);
    }

    private boolean isConsistsOfPartRelation(RelationMappingDto relation) {
        return relation.relType() == RelationshipType.CONSISTS_OF && "Part".equals(relation.targetLabel());
    }

    private boolean isSuppliedBySupplierRelation(RelationMappingDto relation) {
        return relation.relType() == RelationshipType.SUPPLIED_BY && "Supplier".equals(relation.targetLabel());
    }

    private boolean isDefinedByDrawingRelation(RelationMappingDto relation) {
        return relation.relType() == RelationshipType.DEFINED_BY && "Drawing".equals(relation.targetLabel());
    }

    private boolean isHasItemProjectRelation(RelationMappingDto relation) {
        return relation.relType() == RelationshipType.HAS_ITEM && "Project".equals(relation.targetLabel());
    }

    private PartRowValues resolveChildPartValues(Map<String, Object> row, List<PropertyMappingDto> properties) {
        String partNumber = null;
        String name = null;
        String category = null;
        String material = null;
        String unit = null;
        String description = null;

        for (PropertyMappingDto property : properties) {
            String targetProperty = property.targetProperty();
            if (targetProperty == null || targetProperty.isBlank()) {
                continue;
            }

            String value = resolveMappedText(row, property.sourceColumn(), property.dataType());
            if (value == null) {
                continue;
            }

            switch (targetProperty) {
                case "part_number" -> partNumber = value;
                case "name" -> name = value;
                case "category" -> category = value;
                case "material" -> material = value;
                case "unit" -> unit = value;
                case "description" -> description = value;
                default -> {
                }
            }
        }

        return new PartRowValues(partNumber, name, category, material, unit, description);
    }

    private String resolveParentPartNumber(
            Map<String, Object> row,
            RelationMappingDto relation,
            Map<String, String> rootContext
    ) {
        String value = resolveMappedText(row, relation.nodeColumns().get("part_number"), PropertyDataType.STRING);
        if (value != null) {
            return value;
        }

        if (!relation.nodeColumns().isEmpty()) {
            return null;
        }

        if (rootContext == null || rootContext.isEmpty()) {
            return null;
        }

        return normalizeText(rootContext.get("Part"));
    }

    private String resolveSupplierName(
            Map<String, Object> row,
            RelationMappingDto relation,
            Map<String, String> rootContext
    ) {
        String value = resolveMappedText(row, relation.nodeColumns().get("company_name"), PropertyDataType.STRING);
        if (value != null) {
            return value;
        }

        if (!relation.nodeColumns().isEmpty()) {
            return null;
        }

        if (rootContext == null || rootContext.isEmpty()) {
            return null;
        }

        return normalizeText(rootContext.get("Supplier"));
    }

    private DrawingRowValues resolveDrawingValues(
            Map<String, Object> row,
            RelationMappingDto relation,
            Map<String, String> rootContext
    ) {
        String drawingNumber = resolveMappedText(row, relation.nodeColumns().get("drawing_number"), PropertyDataType.STRING);
        String name = resolveMappedText(row, relation.nodeColumns().get("name"), PropertyDataType.STRING);
        String version = resolveMappedText(row, relation.nodeColumns().get("version"), PropertyDataType.STRING);
        DrawingStatus status = resolveDrawingStatus(row, relation.nodeColumns().get("status"));

        if (drawingNumber != null) {
            return new DrawingRowValues(drawingNumber, name, version, status);
        }

        if (!relation.nodeColumns().isEmpty()) {
            return null;
        }

        if (rootContext == null || rootContext.isEmpty()) {
            return null;
        }

        String rootDrawingNumber = normalizeText(rootContext.get("Drawing"));
        if (rootDrawingNumber == null) {
            return null;
        }

        return new DrawingRowValues(rootDrawingNumber, rootDrawingNumber, null, null);
    }

    private ProjectRowValues resolveProjectValues(
            Map<String, Object> row,
            RelationMappingDto relation,
            Map<String, String> rootContext,
            File sourceFile
    ) {
        String name = resolveMappedText(row, relation.nodeColumns().get("name"), PropertyDataType.STRING);
        if (name != null) {
            return new ProjectRowValues(name, null);
        }

        if (!relation.nodeColumns().isEmpty()) {
            return null;
        }

        if (rootContext != null && !rootContext.isEmpty()) {
            String rootProjectName = normalizeText(rootContext.get("Project"));
            if (rootProjectName != null) {
                return new ProjectRowValues(rootProjectName, null);
            }
        }

        if (sourceFile == null || !"project".equals(sourceFile.getOwnerType()) || sourceFile.getOwnerId() == null) {
            return null;
        }

        Project ownerProject = projectRepository.findByIdAndDeletedFalse(sourceFile.getOwnerId()).orElse(null);
        if (ownerProject == null) {
            return null;
        }

        return new ProjectRowValues(ownerProject.getName(), ownerProject);
    }

    private BigDecimal resolveQuantity(Map<String, Object> row, RelationMappingDto relation) {
        String column = relation.relColumns().get("quantity");
        if (column == null || column.isBlank()) {
            return BigDecimal.ONE;
        }

        PropertyDataType dataType = relation.relColumnTypes().getOrDefault("quantity", PropertyDataType.INTEGER);
        Object value = castValue(row.get(column), dataType);
        BigDecimal quantity = toBigDecimal(value);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return quantity;
    }

    private String resolveBomLineNumber(Map<String, Object> row, RelationMappingDto relation) {
        for (String propertyName : List.of("line_number", "find_number", "sequence")) {
            String column = relation.relColumns().get(propertyName);
            if (column == null || column.isBlank()) {
                continue;
            }
            PropertyDataType dataType = relation.relColumnTypes().getOrDefault(propertyName, PropertyDataType.STRING);
            Object value = castValue(row.get(column), dataType);
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private Double resolveUnitCost(Map<String, Object> row, RelationMappingDto relation) {
        String column = relation.relColumns().get("unit_cost");
        if (column == null || column.isBlank()) {
            return null;
        }

        PropertyDataType dataType = relation.relColumnTypes().getOrDefault("unit_cost", PropertyDataType.FLOAT);
        Object value = castValue(row.get(column), dataType);
        return toDouble(value);
    }

    private DrawingStatus resolveDrawingStatus(Map<String, Object> row, String columnName) {
        String raw = resolveMappedText(row, columnName, PropertyDataType.STRING);
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

    private Map<String, Object> resolveRelationExtendedProperties(
            Map<String, Object> row,
            RelationMappingDto relation,
            String... ignoredPropertyNames
    ) {
        Map<String, Object> properties = new LinkedHashMap<>();
        Set<String> ignored = new HashSet<>(List.of(ignoredPropertyNames));
        for (Map.Entry<String, String> entry : relation.relColumns().entrySet()) {
            String propertyName = entry.getKey();
            if (propertyName == null || propertyName.isBlank() || ignored.contains(propertyName)) {
                continue;
            }

            PropertyDataType dataType = relation.relColumnTypes().getOrDefault(propertyName, PropertyDataType.STRING);
            Object value = castValue(row.get(entry.getValue()), dataType);
            if (value == null) {
                continue;
            }

            properties.put(propertyName, value);
        }
        return properties;
    }

    private UpsertPartResult upsertPart(PartRowValues values, boolean overwrite, UUID jobId) {
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
            if (changed) {
                recordSynthesisImport(revision, jobId);
                partRevisionRepository.save(revision);
            }
            return new UpsertPartResult(existing, revision, false);
        }

        Part created = Part.create(values.partNumber());
        partRepository.save(created);
        PartRevision revision = PartRevision.createInitialDraft(created, "D1", values.name());
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
        recordSynthesisImport(revision, jobId);
        partRevisionRepository.save(revision);
        return new UpsertPartResult(created, revision, true);
    }

    private PartRevision findOrCreateWorkingRevision(Part part, String name) {
        PartRevision revision = resolveCurrentRevision(part);
        if (revision != null) {
            return revision;
        }
        PartRevision created = PartRevision.createInitialDraft(part, "D1", name);
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

    private UpsertDrawingResult upsertDrawing(DrawingRowValues values, boolean overwrite) {
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
            return new UpsertDrawingResult(existing, false);
        }

        String drawingName = values.name() != null ? values.name() : values.drawingNumber();
        Drawing created = Drawing.create(values.drawingNumber(), drawingName);
        if (values.version() != null) {
            created.changeVersion(values.version());
        }
        if (values.status() != null) {
            created.changeStatus(values.status());
        }
        drawingRepository.save(created);
        return new UpsertDrawingResult(created, true);
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

    private boolean upsertEngineeringBomItem(
            PartRevision parentRevision,
            PartRevision childRevision,
            String requestedLineNumber,
            BigDecimal quantity,
            Map<String, Object> extendedProperties,
            boolean overwrite
    ) {
        EngineeringBomItem existing = resolveExistingEngineeringBomItem(
                parentRevision,
                childRevision,
                requestedLineNumber
        );
        if (existing != null) {
            boolean changed = false;
            if (requestedLineNumber != null && overwrite && !requestedLineNumber.equals(existing.getLineNumber())) {
                existing.changeLineNumber(requestedLineNumber);
                changed = true;
            }
            if (overwrite && !existing.getChildPartRevisionId().equals(childRevision.getId())) {
                existing.changeChildPartRevision(childRevision.getId());
                changed = true;
            }
            if (overwrite && existing.getQuantity().compareTo(quantity) != 0) {
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

        String lineNumber = requestedLineNumber == null || requestedLineNumber.isBlank()
                ? generateNextEngineeringBomLineNumber(parentRevision.getId())
                : requestedLineNumber;
        engineeringBomItemRepository.save(EngineeringBomItem.add(
                parentRevision.getId(),
                lineNumber,
                childRevision.getId(),
                quantity,
                serializeProperties(extendedProperties)
        ));
        return true;
    }

    private EngineeringBomItem resolveExistingEngineeringBomItem(
            PartRevision parentRevision,
            PartRevision childRevision,
            String requestedLineNumber
    ) {
        if (requestedLineNumber != null && !requestedLineNumber.isBlank()) {
            return engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(
                    parentRevision.getId(),
                    requestedLineNumber
            ).orElse(null);
        }

        List<EngineeringBomItem> existingItems = engineeringBomItemRepository
                .findByParentPartRevisionIdAndChildPartRevisionIdOrderByCreatedAtAsc(
                        parentRevision.getId(),
                        childRevision.getId()
                );
        if (existingItems.size() == 1) {
            return existingItems.get(0);
        }
        return null;
    }

    private String generateNextEngineeringBomLineNumber(UUID parentPartRevisionId) {
        int max = engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(parentPartRevisionId).stream()
                .map(EngineeringBomItem::getLineNumber)
                .map(this::toLineNumberValue)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        return String.valueOf(max <= 0 ? 10 : max + 10);
    }

    private Integer toLineNumberValue(String lineNumber) {
        if (lineNumber == null || lineNumber.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(lineNumber.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private UpsertSupplierResult upsertSupplier(String companyName) {
        Supplier existing = supplierRepository.findByCompanyName(companyName).orElse(null);
        if (existing != null) {
            return new UpsertSupplierResult(existing, false);
        }

        Supplier created = Supplier.create(companyName, null, null, null, "{}");
        supplierRepository.save(created);
        return new UpsertSupplierResult(created, true);
    }

    private boolean upsertPartSupplier(
            PartRevision partRevision,
            Supplier supplier,
            Double unitCost,
            Map<String, Object> extendedProperties,
            boolean overwrite
    ) {
        PartSupplier existing = partSupplierRepository
                .findByPartRevisionIdAndSupplierId(partRevision.getId(), supplier.getId())
                .orElse(null);
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
                partRevision.getId(),
                supplier.getId(),
                unitCost,
                serializeProperties(extendedProperties)
        ));
        return true;
    }

    private UpsertProjectResult upsertProject(ProjectRowValues values) {
        if (values.existingProject() != null) {
            return new UpsertProjectResult(values.existingProject(), false);
        }

        Project existing = projectRepository.findByNameAndDeletedFalse(values.name()).orElse(null);
        if (existing != null) {
            return new UpsertProjectResult(existing, false);
        }

        Project created = Project.create(values.name(), null);
        projectRepository.save(created);
        return new UpsertProjectResult(created, true);
    }

    private boolean linkPartRevisionToDrawing(PartRevision partRevision, Drawing drawing) {
        if (partRevision == null) {
            return false;
        }
        if (partRevision.getId().equals(drawing.getPartRevisionId())) {
            return false;
        }

        drawing.assignPartRevision(partRevision.getId());
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

    private MappingResultDto parseMapping(String rawMapping) {
        if (rawMapping == null || rawMapping.isBlank()) {
            return new MappingResultDto(List.of(), List.of());
        }

        try {
            return objectMapper.readValue(rawMapping, MappingResultDto.class);
        } catch (JacksonException ex) {
            return new MappingResultDto(List.of(), List.of());
        }
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

    private String resolveMappedText(Map<String, Object> row, String columnName, PropertyDataType dataType) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        Object value = castValue(row.get(columnName), dataType);
        return normalizeText(value);
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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimalValue) {
            return decimalValue.stripTrailingZeros();
        }
        if (value instanceof Number numberValue) {
            return new BigDecimal(numberValue.toString()).stripTrailingZeros();
        }
        try {
            return new BigDecimal(value.toString().trim()).stripTrailingZeros();
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
            PartRevision revision,
            boolean created
    ) {
    }

    private record UpsertSupplierResult(
            Supplier supplier,
            boolean created
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

    private record PartRowValues(
            String partNumber,
            String name,
            String category,
            String material,
            String unit,
            String description
    ) {
    }

    private record DrawingRowValues(
            String drawingNumber,
            String name,
            String version,
            DrawingStatus status
    ) {
    }

    private record ProjectRowValues(
            String name,
            Project existingProject
    ) {
    }

    private record MergedPropertiesResult(
            String serialized,
            boolean changed
    ) {
    }
}
