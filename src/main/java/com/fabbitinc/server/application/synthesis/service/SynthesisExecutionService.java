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
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import com.fabbitinc.server.domain.mapping.repository.MappingRevisionRepository;
import com.fabbitinc.server.domain.part.model.BomLink;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartSupplier;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.part.repository.PartSupplierRepository;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
    private final BomLinkRepository bomLinkRepository;
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
            job.markProcessing();

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
            job.setTotalRows(rows.size());

            if (rows.isEmpty()) {
                job.replaceErrors("[]");
                job.markCompleted();
                return;
            }

            int processedRows = 0;
            int nodesCreated = 0;
            int relationshipsCreated = 0;
            List<String> errors = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                processedRows++;
                try {
                    RowProcessResult result = processRow(row, mapping, rootContext, overwrite, job.getId());
                    nodesCreated += result.nodesCreated();
                    relationshipsCreated += result.relationshipsCreated();
                } catch (Exception ex) {
                    String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    errors.add("행 " + processedRows + ": " + message);
                }
            }

            job.incrementUsageProgress(processedRows, nodesCreated, relationshipsCreated);
            job.replaceErrors(serializeErrors(errors));
            job.markCompleted();
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            job.markFailed(serializeErrors(List.of(message)));
        }
    }

    private RowProcessResult processRow(
            Map<String, Object> row,
            MappingResultDto mapping,
            Map<String, String> rootContext,
            boolean overwrite,
            UUID jobId
    ) {
        int nodesCreated = 0;
        int relationshipsCreated = 0;

        String childPartNumber = resolveChildPartNumber(row, mapping.propertyMappings());
        if (childPartNumber == null) {
            return new RowProcessResult(0, 0);
        }

        String childName = resolvePartName(row, mapping.propertyMappings());
        UpsertPartResult childResult = upsertPart(childPartNumber, childName, overwrite, jobId);
        if (childResult.created()) {
            nodesCreated++;
        }

        for (RelationMappingDto relation : mapping.relationMappings()) {
            if (isConsistsOfPartRelation(relation)) {
                String parentPartNumber = resolveParentPartNumber(row, relation, rootContext);
                if (parentPartNumber == null || parentPartNumber.equals(childPartNumber)) {
                    continue;
                }

                String parentName = resolveMappedText(row, relation.nodeColumns().get("name"), PropertyDataType.STRING);
                UpsertPartResult parentResult = upsertPart(parentPartNumber, parentName, overwrite, jobId);
                if (parentResult.created()) {
                    nodesCreated++;
                }

                int quantity = resolveQuantity(row, relation);
                if (upsertBomLink(parentResult.part(), childResult.part(), quantity)) {
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
                if (upsertPartSupplier(childResult.part(), supplierResult.supplier(), unitCost)) {
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

    private String resolveChildPartNumber(Map<String, Object> row, List<PropertyMappingDto> properties) {
        for (PropertyMappingDto property : properties) {
            if (!"part_number".equals(property.targetProperty())) {
                continue;
            }
            String value = resolveMappedText(row, property.sourceColumn(), property.dataType());
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String resolvePartName(Map<String, Object> row, List<PropertyMappingDto> properties) {
        for (PropertyMappingDto property : properties) {
            if (!"name".equals(property.targetProperty())) {
                continue;
            }
            String value = resolveMappedText(row, property.sourceColumn(), property.dataType());
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private int resolveQuantity(Map<String, Object> row, RelationMappingDto relation) {
        String column = relation.relColumns().get("quantity");
        if (column == null || column.isBlank()) {
            return 1;
        }

        PropertyDataType dataType = relation.relColumnTypes().getOrDefault("quantity", PropertyDataType.INTEGER);
        Object value = castValue(row.get(column), dataType);
        Integer quantity = toInteger(value);
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        return quantity;
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

    private UpsertPartResult upsertPart(String partNumber, String name, boolean overwrite, UUID jobId) {
        Part existing = partRepository.findByPartNumber(partNumber).orElse(null);
        if (existing != null) {
            boolean changed = false;
            if (name != null
                    && (overwrite || existing.getName() == null || existing.getName().isBlank())
                    && !name.equals(existing.getName())) {
                existing.changeName(name);
                changed = true;
            }
            if (changed) {
                existing.bumpRevision();
                partRevisionRepository.save(PartRevision.capture(existing, jobId));
            }
            return new UpsertPartResult(existing, false);
        }

        Part created = Part.create(partNumber, name);
        partRepository.save(created);
        partRevisionRepository.save(PartRevision.capture(created, jobId));
        return new UpsertPartResult(created, true);
    }

    private boolean upsertBomLink(Part parent, Part child, int quantity) {
        boolean exists = bomLinkRepository.findByParentPartIdAndChildPartId(parent.getId(), child.getId()).isPresent();
        if (exists) {
            return false;
        }

        bomLinkRepository.save(BomLink.connect(parent, child, quantity, "{}"));
        return true;
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

    private boolean upsertPartSupplier(Part part, Supplier supplier, Double unitCost) {
        boolean exists = partSupplierRepository.findByPartIdAndSupplierId(part.getId(), supplier.getId()).isPresent();
        if (exists) {
            return false;
        }

        partSupplierRepository.save(PartSupplier.link(part, supplier, unitCost, "{}"));
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
            boolean created
    ) {
    }
}
