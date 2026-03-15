package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.model.RelationMappingDto;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MappingGenerationSupport {

    public MappingResultDto generate(
            List<String> headers,
            List<Map<String, Object>> sampleRows
    ) {
        List<PropertyMappingDto> propertyMappings = new ArrayList<>();
        List<RelationMappingDto> relationMappings = new ArrayList<>();
        Set<String> usedColumns = new LinkedHashSet<>();

        addProperty(propertyMappings, usedColumns, headers,
                List.of("품번", "part_number", "part number", "part no", "partno", "p/n", "pn"),
                List.of("상위", "parent", "supplier", "vendor", "drawing", "project"),
                "part_number",
                PropertyDataType.STRING,
                95,
                "main part number"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("품명", "name", "part_name", "part name"),
                List.of("상위", "parent", "supplier", "vendor", "drawing", "project"),
                "name",
                PropertyDataType.STRING,
                92,
                "main part name"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("revision", "rev", "리비전"),
                List.of("도면", "drawing", "project"),
                "revision",
                PropertyDataType.STRING,
                88,
                "revision field"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("재질", "material"),
                List.of("supplier", "drawing", "project"),
                "material",
                PropertyDataType.STRING,
                85,
                "material field"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("단위", "unit"),
                List.of("단가", "cost", "supplier", "project"),
                "unit",
                PropertyDataType.STRING,
                82,
                "unit field"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("설명", "description", "desc"),
                List.of("drawing", "project"),
                "description",
                PropertyDataType.STRING,
                80,
                "description field"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("분류", "category"),
                List.of("project"),
                "category",
                PropertyDataType.STRING,
                80,
                "category field"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("phantom", "팬텀"),
                List.of("project"),
                "is_phantom",
                PropertyDataType.BOOLEAN,
                78,
                "phantom indicator"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("lifecycle", "state", "수명주기"),
                List.of("project", "도면", "drawing"),
                "lifecycle_state",
                PropertyDataType.STRING,
                75,
                "lifecycle state"
        );
        addProperty(propertyMappings, usedColumns, headers,
                List.of("lead time", "리드타임"),
                List.of("project"),
                "lead_time_days",
                PropertyDataType.INTEGER,
                78,
                "lead time"
        );

        RelationMappingDto consistsOf = buildConsistsOf(headers);
        if (hasRelationData(consistsOf)) {
            relationMappings.add(consistsOf);
            usedColumns.addAll(consistsOf.nodeColumns().values());
            usedColumns.addAll(consistsOf.relColumns().values());
        }

        RelationMappingDto suppliedBy = buildSuppliedBy(headers);
        if (hasRelationData(suppliedBy)) {
            relationMappings.add(suppliedBy);
            usedColumns.addAll(suppliedBy.nodeColumns().values());
            usedColumns.addAll(suppliedBy.relColumns().values());
        }

        RelationMappingDto definedBy = buildDefinedBy(headers);
        if (hasRelationData(definedBy)) {
            relationMappings.add(definedBy);
            usedColumns.addAll(definedBy.nodeColumns().values());
            usedColumns.addAll(definedBy.relColumns().values());
        }

        RelationMappingDto hasItem = buildHasItem(headers);
        if (hasRelationData(hasItem)) {
            relationMappings.add(hasItem);
            usedColumns.addAll(hasItem.nodeColumns().values());
            usedColumns.addAll(hasItem.relColumns().values());
        }

        int extIndex = 1;
        for (String header : headers) {
            if (usedColumns.contains(header)) {
                continue;
            }
            if (isColumnEffectivelyEmpty(sampleRows, header)) {
                continue;
            }

            String extKey = toExtendedPropertyName(header, extIndex++);
            propertyMappings.add(new PropertyMappingDto(
                    header,
                    extKey,
                    extKey,
                    PropertyDataType.STRING,
                    60,
                    "extended property",
                    true
            ));
            usedColumns.add(header);
        }

        return new MappingResultDto(propertyMappings, relationMappings);
    }

    private void addProperty(
            List<PropertyMappingDto> propertyMappings,
            Set<String> usedColumns,
            List<String> headers,
            List<String> includeKeywords,
            List<String> excludeKeywords,
            String targetProperty,
            PropertyDataType dataType,
            int confidence,
            String reason
    ) {
        String selected = selectHeader(headers, includeKeywords, excludeKeywords, usedColumns);
        if (selected == null) {
            return;
        }

        propertyMappings.add(new PropertyMappingDto(
                selected,
                targetProperty,
                null,
                dataType,
                confidence,
                reason,
                false
        ));
        usedColumns.add(selected);
    }

    private String selectHeader(
            List<String> headers,
            List<String> includeKeywords,
            List<String> excludeKeywords,
            Set<String> usedColumns
    ) {
        String bestHeader = null;
        int bestScore = Integer.MIN_VALUE;

        for (String header : headers) {
            if (usedColumns.contains(header)) {
                continue;
            }

            String normalized = normalize(header);
            int score = 0;
            boolean matched = false;

            for (String includeKeyword : includeKeywords) {
                String keyword = normalize(includeKeyword);
                if (normalized.contains(keyword)) {
                    score += 10;
                    matched = true;
                }
            }

            if (!matched) {
                continue;
            }

            for (String excludeKeyword : excludeKeywords) {
                String keyword = normalize(excludeKeyword);
                if (normalized.contains(keyword)) {
                    score -= 6;
                }
            }

            if (normalized.contains("하위") || normalized.contains("child")) {
                score += 4;
            }

            if (score > bestScore) {
                bestScore = score;
                bestHeader = header;
            }
        }

        return bestHeader;
    }

    private RelationMappingDto buildConsistsOf(List<String> headers) {
        Map<String, String> nodeColumns = new LinkedHashMap<>();
        Map<String, String> relColumns = new LinkedHashMap<>();
        Map<String, PropertyDataType> relColumnTypes = new LinkedHashMap<>();

        String parentPartNumber = findHeader(headers, List.of("상위품번", "parent part", "parent pn", "상위 pn"));
        if (parentPartNumber != null) {
            nodeColumns.put("part_number", parentPartNumber);
        }

        String parentPartName = findHeader(headers, List.of("상위품명", "parent name", "상위명"));
        if (parentPartName != null) {
            nodeColumns.put("name", parentPartName);
        }

        String quantity = findHeader(headers, List.of("수량", "qty", "quantity"));
        if (quantity != null) {
            relColumns.put("quantity", quantity);
            relColumnTypes.put("quantity", PropertyDataType.FLOAT);
        }

        return new RelationMappingDto(
                RelationshipType.CONSISTS_OF,
                "Part",
                nodeColumns,
                relColumns,
                relColumnTypes,
                86,
                "bom relationship"
        );
    }

    private RelationMappingDto buildSuppliedBy(List<String> headers) {
        Map<String, String> nodeColumns = new LinkedHashMap<>();
        Map<String, String> relColumns = new LinkedHashMap<>();
        Map<String, PropertyDataType> relColumnTypes = new LinkedHashMap<>();

        String supplierName = findHeader(headers, List.of("업체명", "공급사", "supplier", "vendor", "company"));
        if (supplierName != null) {
            nodeColumns.put("company_name", supplierName);
        }

        String supplierCode = findHeader(headers, List.of("업체코드", "supplier code", "vendor code", "code"));
        if (supplierCode != null) {
            nodeColumns.put("code", supplierCode);
        }

        String country = findHeader(headers, List.of("국가", "country"));
        if (country != null) {
            nodeColumns.put("country", country);
        }

        String unitCost = findHeader(headers, List.of("단가", "unit cost", "cost"));
        if (unitCost != null) {
            relColumns.put("unit_cost", unitCost);
            relColumnTypes.put("unit_cost", PropertyDataType.FLOAT);
        }

        return new RelationMappingDto(
                RelationshipType.SUPPLIED_BY,
                "Supplier",
                nodeColumns,
                relColumns,
                relColumnTypes,
                82,
                "supplier relationship"
        );
    }

    private RelationMappingDto buildDefinedBy(List<String> headers) {
        Map<String, String> nodeColumns = new LinkedHashMap<>();

        String drawingNumber = findHeader(headers, List.of("도면번호", "drawing number", "drawing no", "dwg"));
        if (drawingNumber != null) {
            nodeColumns.put("drawing_number", drawingNumber);
        }

        String drawingName = findHeader(headers, List.of("도면명", "drawing name"));
        if (drawingName != null) {
            nodeColumns.put("name", drawingName);
        }

        String version = findHeader(headers, List.of("도면버전", "drawing version"));
        if (version != null) {
            nodeColumns.put("version", version);
        }

        String status = findHeader(headers, List.of("도면상태", "drawing status"));
        if (status != null) {
            nodeColumns.put("status", status);
        }

        return new RelationMappingDto(
                RelationshipType.DEFINED_BY,
                "Drawing",
                nodeColumns,
                Map.of(),
                Map.of(),
                80,
                "drawing relationship"
        );
    }

    private RelationMappingDto buildHasItem(List<String> headers) {
        Map<String, String> nodeColumns = new LinkedHashMap<>();

        String projectName = findHeader(headers, List.of("프로젝트명", "project name", "project"));
        if (projectName != null) {
            nodeColumns.put("name", projectName);
        }

        String projectCode = findHeader(headers, List.of("프로젝트코드", "project code"));
        if (projectCode != null) {
            nodeColumns.put("project_code", projectCode);
        }

        String manager = findHeader(headers, List.of("담당자", "pm", "manager"));
        if (manager != null) {
            nodeColumns.put("manager", manager);
        }

        String targetDate = findHeader(headers, List.of("목표일", "target date"));
        if (targetDate != null) {
            nodeColumns.put("target_date", targetDate);
        }

        String status = findHeader(headers, List.of("프로젝트상태", "project status"));
        if (status != null) {
            nodeColumns.put("status", status);
        }

        return new RelationMappingDto(
                RelationshipType.HAS_ITEM,
                "Project",
                nodeColumns,
                Map.of(),
                Map.of(),
                75,
                "project relationship"
        );
    }

    private String findHeader(List<String> headers, List<String> keywords) {
        for (String header : headers) {
            String normalized = normalize(header);
            for (String keyword : keywords) {
                if (normalized.contains(normalize(keyword))) {
                    return header;
                }
            }
        }
        return null;
    }

    private boolean hasRelationData(RelationMappingDto relationMapping) {
        return !relationMapping.nodeColumns().isEmpty() || !relationMapping.relColumns().isEmpty();
    }

    private boolean isColumnEffectivelyEmpty(List<Map<String, Object>> sampleRows, String column) {
        for (Map<String, Object> sampleRow : sampleRows) {
            Object value = sampleRow.get(column);
            if (value != null && !String.valueOf(value).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String toExtendedPropertyName(String header, int index) {
        String lower = header == null ? "" : header.toLowerCase(Locale.ROOT);
        String ascii = lower.replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_", "")
                .replaceAll("_$", "");

        if (ascii.isBlank()) {
            ascii = "col_" + index;
        }
        return "_ext_" + ascii;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
