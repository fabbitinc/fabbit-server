package com.fabbitinc.server.application.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "조직 멤버 lookup 응답")
public record MemberLookupResponse(
        @Schema(description = "멤버 후보 목록")
        List<MemberLookupItemResponse> items
) {
}
