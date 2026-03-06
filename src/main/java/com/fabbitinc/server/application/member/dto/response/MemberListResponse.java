package com.fabbitinc.server.application.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "조직 멤버 목록 응답")
public record MemberListResponse(
        @Schema(description = "조직 멤버 목록")
        List<MemberSummaryResponse> items,
        @Schema(description = "현재 플랜의 최대 멤버 수", example = "50")
        int maxMembers
) {
}
