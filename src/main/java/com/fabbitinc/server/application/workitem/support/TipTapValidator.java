package com.fabbitinc.server.application.workitem.support;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class TipTapValidator {

    private static final Set<String> ALLOWED_NODE_TYPES = Set.of(
            "doc", "paragraph", "text", "heading",
            "bulletList", "orderedList", "listItem", "taskList", "taskItem",
            "blockquote", "codeBlock", "hardBreak", "horizontalRule",
            "image", "mention", "userMention", "issueMention",
            "table", "tableRow", "tableCell", "tableHeader"
    );

    private static final Set<String> ALLOWED_MARK_TYPES = Set.of(
            "bold", "italic", "strike", "underline",
            "code", "link", "highlight", "textStyle",
            "superscript", "subscript"
    );

    public void validateDocument(JsonNode body) {
        if (body == null || body.isNull()) {
            return;
        }
        if (!"doc".equals(body.path("type").asText(null))) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "최상위 타입은 'doc'이어야 합니다");
        }
        validateNode(body);
    }

    private void validateNode(JsonNode node) {
        String type = node.path("type").asText(null);
        if (type == null || !ALLOWED_NODE_TYPES.contains(type)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 노드 타입: " + type);
        }

        if ("image".equals(type)) {
            validateImage(node);
        }
        if ("userMention".equals(type) || "issueMention".equals(type)) {
            validateMention(node, type);
        }

        JsonNode marks = node.path("marks");
        if (marks.isArray()) {
            for (JsonNode mark : marks) {
                validateMark(mark);
            }
        }

        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode child : content) {
                if (child.isObject()) {
                    validateNode(child);
                }
            }
        }
    }

    private void validateMark(JsonNode mark) {
        String type = mark.path("type").asText(null);
        if (type == null || !ALLOWED_MARK_TYPES.contains(type)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 마크 타입: " + type);
        }

        if (!"link".equals(type)) {
            return;
        }

        JsonNode attrs = mark.path("attrs");
        if (!attrs.isObject()) {
            return;
        }

        String href = attrs.path("href").asText("");
        if (!href.isBlank()
                && !href.startsWith("http://")
                && !href.startsWith("https://")
                && !href.startsWith("mailto:")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "link href는 http/https/mailto만 허용됩니다");
        }
    }

    private void validateImage(JsonNode node) {
        JsonNode attrs = node.path("attrs");
        if (!attrs.isObject()) {
            return;
        }

        String src = attrs.path("src").asText("");
        if (!src.isBlank() && !src.startsWith("http://") && !src.startsWith("https://")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "image src는 http/https만 허용됩니다");
        }
    }

    private void validateMention(JsonNode node, String mentionType) {
        JsonNode attrs = node.path("attrs");
        if (!attrs.isObject()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, mentionType + "에는 attrs가 필요합니다");
        }

        JsonNode idNode = attrs.get("id");
        JsonNode labelNode = attrs.get("label");
        if (idNode == null || labelNode == null) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    mentionType + " attrs에는 id와 label이 필요합니다"
            );
        }

        try {
            UUID.fromString(idNode.asText());
        } catch (Exception ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, mentionType + " attrs.id는 유효한 UUID여야 합니다");
        }

        if (!labelNode.isTextual()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, mentionType + " attrs.label은 문자열이어야 합니다");
        }
    }
}
