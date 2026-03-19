package com.fabbitinc.server.application.mapping.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.model.ExtendedPropertyMappingDto;
import com.fabbitinc.server.application.mapping.model.NodeMappingDto;
import com.fabbitinc.server.application.mapping.query.MappingQuery;
import com.fabbitinc.server.application.mapping.query.condition.MappingDetailCondition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MappingApi {

    private final MappingQuery mappingQuery;

    public Map<String, String> getPartExportHeaderAliases(UUID mappingId) {
        if (mappingId == null) {
            return Map.of();
        }

        try {
            Map<String, String> aliases = new LinkedHashMap<>();
            for (NodeMappingDto node : mappingQuery.get(new MappingDetailCondition(mappingId)).mapping().nodes()) {
                if (!"Part".equals(node.label())) {
                    continue;
                }

                node.propertyColumns().forEach((propertyName, sourceColumn) -> putIfPresent(aliases, propertyName, sourceColumn));
                for (ExtendedPropertyMappingDto property : node.extendedProperties()) {
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
