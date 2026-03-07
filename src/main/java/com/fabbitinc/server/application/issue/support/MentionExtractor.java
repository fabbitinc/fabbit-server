package com.fabbitinc.server.application.issue.support;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class MentionExtractor {

    public MentionSet extract(JsonNode body) {
        Set<UUID> userIds = new LinkedHashSet<>();
        Set<UUID> issueIds = new LinkedHashSet<>();

        if (body == null || body.isNull()) {
            return new MentionSet(userIds, issueIds);
        }

        walk(body, userIds, issueIds);
        return new MentionSet(userIds, issueIds);
    }

    private void walk(JsonNode node, Set<UUID> userIds, Set<UUID> issueIds) {
        String type = node.path("type").asText(null);
        if ("userMention".equals(type) || "issueMention".equals(type)) {
            JsonNode attrs = node.path("attrs");
            if (attrs.isObject()) {
                String rawId = attrs.path("id").asText(null);
                if (rawId != null) {
                    try {
                        UUID uuid = UUID.fromString(rawId);
                        if ("userMention".equals(type)) {
                            userIds.add(uuid);
                        } else {
                            issueIds.add(uuid);
                        }
                    } catch (Exception ignored) {
                        // 검증 단계에서 처리되므로 여기서는 무시한다.
                    }
                }
            }
        }

        JsonNode content = node.path("content");
        if (!content.isArray()) {
            return;
        }

        for (JsonNode child : content) {
            if (child.isObject()) {
                walk(child, userIds, issueIds);
            }
        }
    }

    public record MentionSet(
            Set<UUID> userIds,
            Set<UUID> issueIds
    ) {
    }
}
