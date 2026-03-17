package com.fabbitinc.server.application.workitem.query;

import com.fabbitinc.server.application.workitem.query.result.TimelineDetailResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineRefResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineValueChangeResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public final class TimelineDetailParser {

    private TimelineDetailParser() {
    }

    public static TimelineDetailResult parse(JsonNode detail) {
        if (detail == null || detail.isNull() || !detail.isObject()) {
            return null;
        }

        return new TimelineDetailResult(
                parseChanges(detail.get("changes")),
                parseRefs(detail.get("refs")),
                parseRefs(detail.get("added")),
                parseRefs(detail.get("removed"))
        );
    }

    private static Map<String, TimelineValueChangeResult> parseChanges(JsonNode changesNode) {
        if (changesNode == null || changesNode.isNull() || !changesNode.isObject()) {
            return Map.of();
        }

        Map<String, TimelineValueChangeResult> changes = new LinkedHashMap<>();
        changesNode.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || value.isNull() || !value.isObject()) {
                return;
            }
            changes.put(entry.getKey(), new TimelineValueChangeResult(value.get("old"), value.get("new")));
        });
        return Map.copyOf(changes);
    }

    private static List<TimelineRefResult> parseRefs(JsonNode refsNode) {
        if (refsNode == null || refsNode.isNull() || !refsNode.isArray()) {
            return List.of();
        }

        List<TimelineRefResult> refs = new ArrayList<>();
        for (JsonNode refNode : refsNode) {
            if (refNode == null || refNode.isNull() || !refNode.isObject()) {
                continue;
            }
            refs.add(new TimelineRefResult(
                    readText(refNode.get("id")),
                    readText(refNode.get("type")),
                    readText(refNode.get("label")),
                    refNode.get("meta")
            ));
        }
        return List.copyOf(refs);
    }

    private static String readText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
