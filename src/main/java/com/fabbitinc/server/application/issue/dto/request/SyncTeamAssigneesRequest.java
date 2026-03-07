package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "팀 담당자 동기화 요청")
public record SyncTeamAssigneesRequest(
        @Schema(description = "최종 팀 ID 목록")
        List<UUID> teamIds
) {
    public SyncTeamAssigneesRequest {
        teamIds = teamIds == null ? List.of() : List.copyOf(teamIds);
    }
}
