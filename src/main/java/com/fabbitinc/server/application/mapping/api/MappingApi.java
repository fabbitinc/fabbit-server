package com.fabbitinc.server.application.mapping.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.query.MappingQuery;
import com.fabbitinc.server.application.mapping.query.condition.MappingDetailCondition;
import com.fabbitinc.server.application.mapping.query.result.PropertyMappingResult;
import com.fabbitinc.server.application.mappingv2.model.ExtendedPropertyMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.model.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.query.MappingV2Query;
import com.fabbitinc.server.application.mappingv2.query.condition.MappingV2DetailCondition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MappingApi {

    private final MappingQuery mappingQuery;
    private final MappingV2Query mappingV2Query;

    public Map<String, String> getPartExportHeaderAliases(UUID mappingId) {
        if (mappingId == null) {
            return Map.of();
        }

        Map<String, String> aliases = getPartExportHeaderAliasesFromV2(mappingId);
        if (!aliases.isEmpty()) {
            return aliases;
        }

        return getPartExportHeaderAliasesFromLegacy(mappingId);
    }

    private Map<String, String> getPartExportHeaderAliasesFromV2(UUID mappingId) {
        try {
            Map<String, String> aliases = new LinkedHashMap<>();
            for (NodeMappingV2Dto node : mappingV2Query.get(new MappingV2DetailCondition(mappingId)).mapping().nodes()) {
                if (!"Part".equals(node.label())) {
                    continue;
                }

                node.propertyColumns().forEach((propertyName, sourceColumn) -> putIfPresent(aliases, propertyName, sourceColumn));
                for (ExtendedPropertyMappingV2Dto property : node.extendedProperties()) {
                    putIfPresent(aliases, property.generatedKey(), property.sourceColumn());
                }
            }
            return aliases;
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.NOT_FOUND) {
                return Map.of();
            }
            throw ex;
        }
    }

    private Map<String, String> getPartExportHeaderAliasesFromLegacy(UUID mappingId) {
        try {
            Map<String, String> aliases = new LinkedHashMap<>();
            for (PropertyMappingResult property : mappingQuery.get(new MappingDetailCondition(mappingId)).mapping().propertyMappings()) {
                String sourceColumn = trimToNull(property.sourceColumn());
                if (sourceColumn == null) {
                    continue;
                }

                putIfPresent(aliases, property.targetProperty(), sourceColumn);
                if (Boolean.TRUE.equals(property.isExtended())) {
                    putIfPresent(aliases, property.suggestedExtendedProperty(), sourceColumn);
                }
            }
            return aliases;
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.NOT_FOUND) {
                return Map.of();
            }
            throw ex;
        }
    }

    private void putIfPresent(Map<String, String> aliases, String propertyName, String sourceColumn) {
        String resolvedPropertyName = trimToNull(propertyName);
        String resolvedSourceColumn = trimToNull(sourceColumn);
        if (resolvedPropertyName == null || resolvedSourceColumn == null) {
            return;
        }
        aliases.putIfAbsent(resolvedPropertyName, resolvedSourceColumn);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
