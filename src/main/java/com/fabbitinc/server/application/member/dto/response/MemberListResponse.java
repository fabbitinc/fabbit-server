package com.fabbitinc.server.application.member.dto.response;

import java.util.List;

public record MemberListResponse(
        List<MemberSummaryResponse> items,
        int maxMembers
) {
}
