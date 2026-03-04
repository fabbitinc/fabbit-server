package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "CR-이슈 동기화 요청")
public record SyncIssuesRequest(
        @Schema(description = "최종 이슈 ID 목록")
        List<UUID> issueIds
) {
    public SyncIssuesRequest {
        issueIds = issueIds == null ? List.of() : List.copyOf(issueIds);
    }
}
