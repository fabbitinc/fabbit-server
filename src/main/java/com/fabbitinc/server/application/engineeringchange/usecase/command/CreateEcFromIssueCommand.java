package com.fabbitinc.server.application.engineeringchange.usecase.command;

import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record CreateEcFromIssueCommand(
        UUID issueId,
        String title,
        JsonNode body,
        List<UUID> reviewerIds,
        List<UUID> approverIds
) {
    public CreateEcFromIssueCommand {
        reviewerIds = reviewerIds == null ? List.of() : List.copyOf(reviewerIds);
        approverIds = approverIds == null ? List.of() : List.copyOf(approverIds);
    }
}
