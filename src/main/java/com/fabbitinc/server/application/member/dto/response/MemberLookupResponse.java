package com.fabbitinc.server.application.member.dto.response;

import java.util.List;

public record MemberLookupResponse(
        List<MemberLookupItemResponse> items
) {
}
